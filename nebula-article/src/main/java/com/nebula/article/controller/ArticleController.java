package com.nebula.article.controller;

import com.nebula.article.entity.Article;
import com.nebula.article.service.ArticleService;
import com.nebula.common.result.Result;
import org.springframework.web.bind.annotation.*;

import static com.nebula.article.entity.table.ArticleTableDef.ARTICLE;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public Result<String> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author
    ) {
        return Result.success("");
//        return articleService.pageArticles(page, size, title, author);
    }

    @GetMapping("/{id}")
    public Article get(@PathVariable Long id) {
        return Article.create().where(ARTICLE.ID.eq(id)).one();
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
        return article
                .where(ARTICLE.ID.eq(article.getId()))
                .update(true);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        Article article = Article.create().where(ARTICLE.ID.eq(id)).one();
        if (article == null) return false;
        article.setStatus(0); // 逻辑删除
        return article.updateById();
    }
}