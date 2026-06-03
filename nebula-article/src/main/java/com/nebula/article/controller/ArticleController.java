package com.nebula.article.controller;

import com.mybatisflex.core.paginate.Page;
import com.nebula.article.dto.ChangeArticleStatusDto;
import com.nebula.article.dto.CreateArticleDto;
import com.nebula.article.dto.UpdateArticleDto;
import com.nebula.article.entity.Article;
import com.nebula.article.service.ArticleService;
import com.nebula.auth.util.SecurityUtils;
import com.nebula.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
@Validated
public class ArticleController {

    private final ArticleService articleService;

    private final SecurityUtils securityUtils;

    /**
     * 分页查询文章列表
     *
     * @param title       文章标题（支持模糊查询）
     * @param author      作者名称
     * @param currentPage 当前页码
     * @param size        每页条数
     * @return 文章分页数据
     */
    @GetMapping
    public Result<Page<Article>> page(@RequestParam(required = false) String title, @RequestParam(required = false) String author, @RequestParam(defaultValue = "1") @Min(1) int currentPage, @Min(1) @Max(100) @RequestParam(defaultValue = "10") int size) {
        return Result.success(articleService.pageArticles(currentPage, size, title, author, null, true));
    }

    /**
     * 根据文章ID查询文章详情
     *
     * @param id 文章ID
     * @return 文章详情
     */
    @GetMapping("/{id}")
    public Result<Article> getArticleById(@PathVariable @Min(value = 1, message = "文章ID非法") Long id) {
        return Result.success(articleService.getArticleById(id));
    }

    /**
     * 新增文章
     *
     * @param dto 新增文章请求参数
     * @return 是否新增成功
     */
    @PostMapping("/create")
    public Result<Boolean> createArticle(@RequestBody @Valid CreateArticleDto dto) {
        return Result.success(Article.create().setAuthor(securityUtils.getLoginUser().getUsername()).setTitle(dto.getTitle()).setContent(dto.getContent()).save());
    }

    /**
     * 修改文章
     *
     * @param dto 修改文章请求参数
     * @return 是否修改成功
     */
    @PutMapping("/edit")
    public Result<Boolean> update(@RequestBody @Valid UpdateArticleDto dto) {
        return Result.success(Article.create().setTitle(dto.getTitle()).setContent(dto.getContent()).setId(dto.getId()).updateById());
    }

    /**
     * 删除文章
     *
     * @param id 文章ID
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable @Min(value = 1, message = "文章ID非法") Long id) {
        return Result.success(Article.create().setId(id).removeById());
    }

    /**
     * 修改文章状态
     *
     * @param dto 修改文章状态请求参数
     * @return 是否修改成功
     */
    @PutMapping("/status")
    public Result<Boolean> changeStatus(@RequestBody @Valid ChangeArticleStatusDto dto) {
        return Result.success(articleService.changeArticleStatus(dto));
    }
}