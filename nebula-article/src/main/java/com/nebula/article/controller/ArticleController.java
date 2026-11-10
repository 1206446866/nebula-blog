package com.nebula.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.mybatisflex.core.paginate.Page;
import com.nebula.article.dto.*;
import com.nebula.article.service.ArticleService;
import com.nebula.article.vo.ArticleVO;
import com.nebula.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
@Validated
@Tag(name = "文章管理", description = "文章查询、写作、草稿、状态和点赞。")
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "分页查询文章",
            description = "需article:list权限，按标题、作者、分类、标签等条件筛选，并按当前账号限制可见范围。")
    @PreAuthorize("@securityUtils.hasPermission('article:list')")
    @GetMapping
    public Result<Page<ArticleVO>> page(@Valid ArticlePageDTO articlePageDTO) {
        return Result.success(articleService.pageArticles(articlePageDTO, null, true));
    }

    @Operation(summary = "查询文章详情",
            description = "需article:list权限，检查文章可见性；正常详情读取可记录浏览量。")
    @PreAuthorize("hasAuthority('article:list')")
    @GetMapping("/{id}")
    public Result<ArticleVO> getArticleById(@Parameter(description = "文章编号") @PathVariable @Min(value = 1, message = "文章ID非法") Long id) {
        return Result.success(articleService.getArticleById(id));
    }


    @Operation(summary = "新增文章",
            description = "需article:create权限，按请求创建文章并检查发布状态与关联资源权限。")
    @PreAuthorize("hasAuthority('article:create')")
    @PostMapping("/create")
    public Result<Boolean> createArticle(@RequestBody @Valid CreateArticleDto dto) {
        return Result.success(articleService.createArticle(dto));
    }

    @Operation(summary = "创建私人草稿",
            description = "需article:create权限，返回草稿编号，供后续继续编辑。")
    @PreAuthorize("hasAuthority('article:create')")
    @PostMapping("/drafts")
    public Result<Long> createDraft(@RequestBody @Valid CreateArticleDto dto) {
        return Result.success(articleService.createDraft(dto));
    }

    @Operation(summary = "读取文章编辑内容",
            description = "需article:update权限并具备目标文章编辑权限；不增加浏览量，不写入公开详情缓存。")
    @PreAuthorize("hasAuthority('article:update')")
    @GetMapping("/{id}/edit")
    public Result<ArticleVO> getArticleForEdit(@Parameter(description = "文章编号") @PathVariable @Min(1) Long id) {
        return Result.success(articleService.getArticleForEdit(id));
    }

    @Operation(summary = "修改文章",
            description = "需article:update权限并具备目标文章编辑权限；按预期版本检查并发修改，拒绝覆盖过期内容。")
    @PreAuthorize("hasAuthority('article:update')")
    @PutMapping("/edit")
    public Result<Boolean> update(@RequestBody @Valid UpdateArticleDto dto){
        return Result.success(articleService.updateArticle(dto));
    }

    @Operation(summary = "删除文章",
            description = "需article:delete权限并具备目标文章管理权限，返回是否删除成功。")
    @PreAuthorize("hasAuthority('article:delete')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@Parameter(description = "文章编号") @PathVariable @Min(value = 1, message = "文章ID非法") Long id) {
        return Result.success(articleService.delete(id));
    }

    @Operation(summary = "管理员修改文章状态",
            description = "需管理员及article:update权限，按合法状态转换规则更新文章。")
    @PreAuthorize("@securityUtils.hasPermission('article:update') && hasRole('ADMIN')")
    @PutMapping("/status")
    public Result<Boolean> changeStatus(@RequestBody @Valid ChangeArticleStatusDto dto) {
        return Result.success(articleService.changeArticleStatus(dto));
    }

    @Operation(summary = "分页查询已发布文章",
            description = "需article:list权限，只返回当前可见的已发布文章。")
    @PreAuthorize("hasAuthority('article:list')")
    @GetMapping("/published")
    public Result<Page<ArticleVO>> pagePublished(@Valid ArticlePageDTO dto) {
        return Result.success(articleService.pagePublishedArticles(dto));
    }

    @Operation(summary = "切换文章点赞状态",
            description = "需已登录，返回切换后的状态：true为已点赞，false为未点赞。")
    @PutMapping("/like")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> like(@RequestBody @Valid ArticleLikeDto articleLikeDto) {
        return Result.success(articleService.like(articleLikeDto));
    }
}
