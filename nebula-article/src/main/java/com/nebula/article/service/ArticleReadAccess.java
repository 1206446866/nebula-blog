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

    /** 生成已发布且未删除的文章条件，供公开统计和关联查询复用。 */
    public QueryCondition publishedCondition() {
        return ARTICLE.DELETED.eq(0).and(ARTICLE.STATUS.eq(ArticleStatus.PUBLISHED.getCode()));
    }

    /** 生成当前用户的文章可见范围，统一保护草稿与逻辑删除数据。 */
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

    /** 构造当前可读文章ID的子查询，供评论等关联业务限制查询范围。 */
    public QueryWrapper readableArticleIds() {
        return QueryWrapper.create().select(ARTICLE.ID).from(ARTICLE).where(readableCondition());
    }

    /** 构造有效已发布文章ID的子查询，供个人主页等公开数据查询使用。 */
    public QueryWrapper publishedArticleIds() {
        return QueryWrapper.create().select(ARTICLE.ID).from(ARTICLE).where(publishedCondition());
    }

    /** 从数据库确认文章当前可读，不可见与不存在统一按文章不存在处理。 */
    public Article requireReadableArticle(Long id) {
        if (id == null || id < 1) throw new BusinessException(ARTICLE_NOT_FOUND);
        Article article = articles.selectOneByQuery(QueryWrapper.create()
                .where(ARTICLE.ID.eq(id)).and(readableCondition()));
        // Do not distinguish a private article from a missing/deleted article to other readers.
        if (article == null) throw new BusinessException(ARTICLE_NOT_FOUND);
        return article;
    }
}
