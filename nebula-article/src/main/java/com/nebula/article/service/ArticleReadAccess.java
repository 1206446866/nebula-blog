package com.nebula.article.service;

import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.nebula.article.entity.Article;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.common.constant.ArticleStatus;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.nebula.article.entity.table.ArticleTableDef.ARTICLE;
import static com.nebula.common.exception.code.ArticleErrorCode.ARTICLE_NOT_FOUND;

/** Shared article visibility for article reads and dependent resources such as comments. */
@Service
@RequiredArgsConstructor
public class ArticleReadAccess {
    private final ArticleMapper articles;

    public QueryCondition publishedCondition() {
        return ARTICLE.DELETED.eq(0).and(ARTICLE.STATUS.eq(ArticleStatus.PUBLISHED.getCode()));
    }

    public QueryCondition readableCondition() {
        if (SecurityUtils.isAdmin() && SecurityUtils.hasPermission("article:list")) {
            return ARTICLE.DELETED.eq(0);
        }
        var viewer = SecurityUtils.getLoginUser();
        QueryCondition visibility = ARTICLE.STATUS.eq(ArticleStatus.PUBLISHED.getCode());
        if (viewer != null && viewer.getUserId() != null) {
            visibility = visibility.or(ARTICLE.USER_ID.eq(viewer.getUserId()));
        }
        return ARTICLE.DELETED.eq(0).and(visibility);
    }

    public QueryWrapper readableArticleIds() {
        return QueryWrapper.create().select(ARTICLE.ID).from(ARTICLE).where(readableCondition());
    }

    public QueryWrapper publishedArticleIds() {
        return QueryWrapper.create().select(ARTICLE.ID).from(ARTICLE).where(publishedCondition());
    }

    public Article requireReadableArticle(Long id) {
        if (id == null || id < 1) throw new BusinessException(ARTICLE_NOT_FOUND);
        Article article = articles.selectOneByQuery(QueryWrapper.create()
                .where(ARTICLE.ID.eq(id)).and(readableCondition()));
        // Do not distinguish a private article from a missing/deleted article to other readers.
        if (article == null) throw new BusinessException(ARTICLE_NOT_FOUND);
        return article;
    }
}
