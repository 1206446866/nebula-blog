package com.nebula.article.controller;

import com.mybatisflex.core.paginate.Page;
import com.nebula.article.dto.ArticlePageDTO;
import com.nebula.article.dto.ChangeArticleStatusDto;
import com.nebula.article.dto.CreateArticleDto;
import com.nebula.article.dto.UpdateArticleDto;
import com.nebula.article.entity.Article;
import com.nebula.article.service.ArticleService;
import com.nebula.article.vo.ArticleVO;
import com.nebula.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.javassist.NotFoundException;
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
    @GetMapping
    public Result<Page<ArticleVO>> page(ArticlePageDTO  articlePageDTO) {
        return Result.success(articleService.pageArticles(articlePageDTO, null, true));
    }

    /**
     * 根据文章ID查询文章详情
     *
     * @param id 文章ID
     * @return 文章详情
     */
    @GetMapping("/{id}")
    public Result<ArticleVO> getArticleById(@PathVariable @Min(value = 1, message = "文章ID非法") Long id) throws NotFoundException {
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
        return Result.success(articleService.createArticle(dto));
    }

    /**
     * 修改文章
     *
     * @param dto 修改文章请求参数
     * @return 是否修改成功
     */
    @PutMapping("/edit")
    public Result<Boolean> update(@RequestBody @Valid UpdateArticleDto dto) {
        return Result.success(articleService.updateArticle(dto));
    }

    /**
     * 删除文章
     *
     * @param id 文章ID
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable @Min(value = 1, message = "文章ID非法") Long id) {
        return Result.success(new Article().setId(id).removeById());
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

    /**
     * 分页查询已发布文章
     * @return 已发布文章分页数据
     */
    @GetMapping("/published")
    public Result<Page<ArticleVO>> pagePublished(ArticlePageDTO dto) {
        return Result.success(articleService.pagePublishedArticles(dto));
    }
}