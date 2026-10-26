package com.nebula.article.controller;

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
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 分页查询文章列表
     *
     * @param articlePageDTO       文章查询接口
     * @return 文章分页数据
     */
    @PreAuthorize("@securityUtils.hasPermission('article:list')")
    @GetMapping
    public Result<Page<ArticleVO>> page(@Valid ArticlePageDTO articlePageDTO) {
        return Result.success(articleService.pageArticles(articlePageDTO, null, true));
    }

    /**
     * 根据文章ID查询文章详情
     *
     * @param id 文章ID
     * @return 文章详情
     */
    @PreAuthorize("hasAuthority('article:list')")
    @GetMapping("/{id}")
    public Result<ArticleVO> getArticleById(@PathVariable @Min(value = 1, message = "文章ID非法") Long id) {
        return Result.success(articleService.getArticleById(id));
    }


    /**
     * 新增文章
     *
     * @param dto 新增文章请求参数
     * @return 是否新增成功
     */
    @PreAuthorize("hasAuthority('article:create')")
    @PostMapping("/create")
    public Result<Boolean> createArticle(@RequestBody @Valid CreateArticleDto dto) {
        return Result.success(articleService.createArticle(dto));
    }

    /** Create a private draft and return its ID for continued editing. */
    @PreAuthorize("hasAuthority('article:create')")
    @PostMapping("/drafts")
    public Result<Long> createDraft(@RequestBody @Valid CreateArticleDto dto) {
        return Result.success(articleService.createDraft(dto));
    }

    /** Authoring reads must not increment views or populate the public detail cache. */
    @PreAuthorize("hasAuthority('article:update')")
    @GetMapping("/{id}/edit")
    public Result<ArticleVO> getArticleForEdit(@PathVariable @Min(1) Long id) {
        return Result.success(articleService.getArticleForEdit(id));
    }

    /**
     * 修改文章
     *
     * @param dto 修改文章请求参数
     * @return 是否修改成功
     */
    @PreAuthorize("hasAuthority('article:update')")
    @PutMapping("/edit")
    public Result<Boolean> update(@RequestBody @Valid UpdateArticleDto dto){
        return Result.success(articleService.updateArticle(dto));
    }

    /**
     * 删除文章
     *
     * @param id 文章ID
     */
    @PreAuthorize("hasAuthority('article:delete')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable @Min(value = 1, message = "文章ID非法") Long id) {
        return Result.success(articleService.delete(id));
    }

    /**
     * 修改文章状态
     *
     * @param dto 修改文章状态请求参数
     * @return 是否修改成功
     */
    @PreAuthorize("@securityUtils.hasPermission('article:update') && hasRole('ADMIN')")
    @PutMapping("/status")
    public Result<Boolean> changeStatus(@RequestBody @Valid ChangeArticleStatusDto dto) {
        return Result.success(articleService.changeArticleStatus(dto));
    }

    /**
     * 分页查询已发布文章
     * @return 已发布文章分页数据
     */
    @PreAuthorize("hasAuthority('article:list')")
    @GetMapping("/published")
    public Result<Page<ArticleVO>> pagePublished(@Valid ArticlePageDTO dto) {
        return Result.success(articleService.pagePublishedArticles(dto));
    }

    @PutMapping("/like")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> like(@RequestBody @Valid ArticleLikeDto articleLikeDto) {
        return Result.success(articleService.like(articleLikeDto));
    }
}
