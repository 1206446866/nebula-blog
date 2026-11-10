package com.nebula.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nebula.article.service.ArticleExportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Validated
@Tag(name = "文章导出", description = "在文章和图片读取权限内导出Markdown或图片压缩包。")
public class ArticleExportController {
    private final ArticleExportService exports;
    public record Input(@NotNull @Size(max=255) String title, @NotNull @Size(max=200000) String content) {}
    @Operation(summary = "导出文章",
            description = "需已登录且有权阅读目标文章；默认下载Markdown，images为true时打包有权读取的本地图片，不抓取外部资源。")
    @GetMapping("/articles/{id}/export")
    public ResponseEntity<byte[]> export(@Parameter(description = "文章编号") @PathVariable @Positive long id, @Parameter(description = "是否将有权读取的本地图片一并打包") @RequestParam(defaultValue="false") boolean images) {
        return response(exports.export(id, images));
    }
    @Operation(summary = "打包编辑器内容",
            description = "需已登录并具备写作权限，将当前正文及有权读取的本地图片打包为ZIP，不保存或发布文章。")
    @PostMapping("/article-export")
    public ResponseEntity<byte[]> archive(@RequestBody @Valid Input input) { return response(exports.archive(input.title(), input.content())); }
    /** 附件下载禁止缓存和浏览器内联解释，文件名由框架编码。 */
    private ResponseEntity<byte[]> response(ArticleExportService.File file) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.name(), StandardCharsets.UTF_8).build().toString())
                .body(file.bytes());
    }
}
