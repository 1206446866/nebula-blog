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

    /** 游客只读取已发布文章，登录后按现有角色和文章归属计算可见范围。 */
    public QueryCondition readableCondition() {
        var viewer = SecurityUtils.getLoginUser();
        if (viewer == null || viewer.getUserId() == null) {
            return publishedCondition();
        }
        if (SecurityUtils.isAdmin() && SecurityUtils.hasPermission("article:list")) {
            return ARTICLE.DELETED.eq(0);
        }
        QueryCondition visibility = ARTICLE.STATUS.eq(ArticleStatus.PUBLISHED.getCode())
                .or(ARTICLE.USER_ID.eq(viewer.getUserId()));
        return ARTICLE.DELETED.eq(0).and(visibility);
    }

    /** 构造当前可读文章ID的子查询，供评论等关联业务限制查询范围。 */
    public QueryWrapper readableArticleIds() {
        return QueryWrapper.create().select(ARTICLE.ID).from(ARTICLE).where(readableCondition());
    }

    /** 构造有效已发布文章ID的子查询，供个人主页等公开数据查询使用。 */
    public QueryWrapper publishedArticleIds() {
        // 显式物化窄编号集合，避免热点评论统计逐条回查含正文的文章聚簇行。
        var published = QueryWrapper.create().select(com.mybatisflex.core.query.QueryMethods.distinct(ARTICLE.ID))
                .from(ARTICLE).where(publishedCondition());
        return QueryWrapper.create().select("published_ids.id").from(published).as("published_ids");
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
