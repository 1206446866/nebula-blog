package com.nebula.media.controller;

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
public class MediaController {
    private final MediaService media;
    private final ImageStorage storage;
    private final ThumbnailService thumbnails;

    @PostMapping("/media/images")
    @PreAuthorize("isAuthenticated()")
    public Result<MediaAsset> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.success(media.uploadArticle(file));
    }
    @GetMapping("/media")
    @PreAuthorize("isAuthenticated()")
    public Result<Page<MediaService.View>> list(@RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "24") int size, @RequestParam(required = false) String business,
            @RequestParam(required = false) String name, @RequestParam(defaultValue = "false") boolean all) {
        return Result.success(media.list(current, size, business, name, all));
    }
    @DeleteMapping("/media/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> delete(@PathVariable Long id) { media.delete(id); return Result.success(true); }
    @GetMapping("/media/{id}/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> file(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean thumbnail) throws IOException {
        var asset = media.requireFile(id);
        if (!thumbnail) return image(asset);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff").body(thumbnails.render(asset));
    }
    @GetMapping("/media/image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FileSystemResource> fileByPath(@RequestParam String path) {
        return image(media.requirePathFile(path));
    }
    @GetMapping("/upload/**")
    public ResponseEntity<FileSystemResource> publicImage(HttpServletRequest request) {
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
