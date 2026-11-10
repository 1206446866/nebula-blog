package com.nebula.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nebula.article.service.ArticleRevisionService;
import com.nebula.article.vo.ArticleRevisionVO;
import com.nebula.common.result.Result;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/articles/{articleId}/revisions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Validated
@Tag(name = "文章历史版本", description = "在编辑权限内查询文章历史正文。")
public class ArticleRevisionController {
    private final ArticleRevisionService revisions;
    @Operation(summary = "查询文章历史版本列表",
            description = "需已登录并具备目标文章编辑权限，返回历史版本摘要。")
    @GetMapping
    public Result<List<ArticleRevisionVO.Summary>> list(@Parameter(description = "文章编号") @PathVariable @Positive long articleId) { return Result.success(revisions.list(articleId)); }
    @Operation(summary = "读取文章历史版本",
            description = "需已登录并具备目标文章编辑权限；只读取指定历史版本，不直接恢复或覆盖当前文章。")
    @GetMapping("/{revisionId}")
    public Result<ArticleRevisionVO.Detail> get(@Parameter(description = "文章编号") @PathVariable @Positive long articleId, @Parameter(description = "历史版本编号") @PathVariable @Positive long revisionId) {
        return Result.success(revisions.get(articleId, revisionId));
    }
}
