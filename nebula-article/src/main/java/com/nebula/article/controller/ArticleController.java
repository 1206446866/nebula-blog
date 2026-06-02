package com.nebula.article.controller;

import com.mybatisflex.core.paginate.Page;
import com.nebula.article.entity.Article;
import com.nebula.article.service.ArticleService;
import com.nebula.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.nebula.article.entity.table.ArticleTableDef.ARTICLE;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public Result<Page<Article>> page(@RequestParam(required = false) String title, @RequestParam(required = false) String author, @RequestParam(defaultValue = "1") int currentPage, @RequestParam(defaultValue = "10") int size) {
        return Result.success(articleService.pageArticles(currentPage, size, title, author, null, true));
    }

    @GetMapping("/{id}")
    public Result<Article> getArticleById(@PathVariable Long id) {
        return Result.success(articleService.getArticleById(id));
    }

    @PostMapping
    public boolean create(@RequestBody Article article) {
        article.setStatus(1); // 默认正常
        return article.save();
    }

    @PutMapping
    public boolean update(@RequestBody Article article) {
        if (article.getId() == null) {
            throw new IllegalArgumentException("id不能为空");
        }
        return article.where(ARTICLE.ID.eq(article.getId())).update(true);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        Article article = Article.create().where(ARTICLE.ID.eq(id)).one();
        if (article == null) return false;
        article.setStatus(0); // 逻辑删除
        return article.updateById();
    }
}