package com.nebula.media.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import com.mybatisflex.core.paginate.Page;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.result.Result;
import com.nebula.media.MediaError;
import com.nebula.media.entity.MediaAsset;
import com.nebula.media.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "图片与媒体", description = "图片上传、媒体库及受权限控制的资源读取。")
public class MediaController {
    private final MediaService media;
    private final ImageStorage storage;
    private final ThumbnailService thumbnails;

    @Operation(summary = "上传文章图片",
            description = "需已登录，检查真实图片、容量、像素、账号配额和磁盘余量，返回媒体记录。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "multipart/form-data",
                            schema = @Schema(type = "object", requiredProperties = {"file"}),
                            schemaProperties = @SchemaProperty(name = "file",
                                    schema = @Schema(type = "string", format = "binary", description = "待上传的图片文件")))))
    @PostMapping("/media/images")
    @PreAuthorize("isAuthenticated()")
    public Result<MediaAsset> upload(@Parameter(description = "待上传的图片文件，最大10MiB，需通过真实图片及像素检查") @RequestParam("file") MultipartFile file) throws IOException {
        return Result.success(media.uploadArticle(file));
    }
    @Operation(summary = "分页查询媒体库",
            description = "需已登录，默认仅本人；all为true时需管理员及相应业务查看权限，受限引用不暴露内容。")
    @GetMapping("/media")
    @PreAuthorize("isAuthenticated()")
    public Result<Page<MediaService.View>> list(@Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页数量，以当前接口校验上限为准") @RequestParam(defaultValue = "24") int size, @Parameter(description = "资源用途：avatar或article") @RequestParam(required = false) String business,
            @Parameter(description = "原始文件名筛选条件") @RequestParam(required = false) String name, @Parameter(description = "是否查看全部媒体，需管理员及对应业务查看权限") @RequestParam(defaultValue = "false") boolean all) {
        return Result.success(media.list(current, size, business, name, all));
    }
    @Operation(summary = "删除闲置图片",
            description = "需已登录并具备资源管理权限；仍被文章、历史版本或有效编辑备份引用的图片不能删除。")
    @DeleteMapping("/media/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> delete(@Parameter(description = "媒体编号") @PathVariable Long id) { media.delete(id); return Result.success(true); }
    @Operation(summary = "按媒体编号读取图片",
            description = "需已登录并具备资源读取权限，可选择读取缩略图，响应禁止缓存。")
    @GetMapping("/media/{id}/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> file(@Parameter(description = "媒体编号") @PathVariable Long id, @Parameter(description = "是否读取缩略图") @RequestParam(defaultValue = "false") boolean thumbnail) throws IOException {
        var asset = media.requireFile(id);
        if (!thumbnail) return image(asset);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff").body(thumbnails.render(asset));
    }
    @Operation(summary = "按相对路径读取图片",
            description = "需已登录并具备资源读取权限，用于读取私人图片，不接受任意磁盘路径。")
    @GetMapping("/media/image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FileSystemResource> fileByPath(@Parameter(description = "upload开头的相对资源路径") @RequestParam String path) {
        return image(media.requirePathFile(path));
    }
    @Operation(summary = "读取公开图片",
            description = "仅允许公开引用的资源及兼容的旧头像；草稿和闲置图片不会因此公开。")
    @GetMapping("/upload/**")
    public ResponseEntity<FileSystemResource> publicImage(@Parameter(hidden = true) HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length() + 1);
        if (path.startsWith("upload/avatar/") && !path.substring(14).contains("/")) {
            Path file = storage.legacyAvatar(path.substring(14));
            String extension = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
            return response(file, "image/" + (extension.equals("jpg") ? "jpeg" : extension));
        }
        return image(media.requirePublicPath(path));
    }
    private ResponseEntity<FileSystemResource> image(MediaAsset asset) {
        return response(storage.resolve(asset.getPath()), asset.getContentType());
    }
    private ResponseEntity<FileSystemResource> response(Path file, String type) {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new BusinessException(MediaError.NOT_FOUND);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .body(new FileSystemResource(file));
    }
}
