package com.nebula.article.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.article.entity.Article;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.article.service.ArticleService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static com.nebula.article.entity.table.ArticleTableDef.ARTICLE;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Override
    public Page<Article> pageArticles(int page, int size, String title, String author, String orderBy, boolean asc) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(ARTICLE.TITLE.eq(title).when(Objects.nonNull(title)&&!title.isEmpty()))
                .and(ARTICLE.AUTHOR.eq(author).when(Objects.nonNull(author)&&!author.isEmpty()));
        if(Objects.nonNull(orderBy)){
                queryWrapper.orderBy(orderBy, asc);
        }
        return page(Page.of(page, size), queryWrapper);
    }

    @Override
    public boolean deleteArticlesBatch(List<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {return true;}
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(ARTICLE.ID.eq(ids));
        // TODO: 使用 QueryWrapper 批量逻辑删除
        return remove(queryWrapper);
    }

    @Override
    public boolean updateStatusBatch(List<Long> ids, Integer status) {
        // TODO: 使用 QueryWrapper 批量更新状态
        return false;
    }

    @Override
    public boolean updateArticlePartial(Long id, Article article) {
        // TODO: 使用 QueryWrapper 更新指定字段
        return false;
    }

    @Override
    public Article getArticleById(Long id) {
        return Article.create().where(ARTICLE.ID.eq(id)).one();
    }
}
