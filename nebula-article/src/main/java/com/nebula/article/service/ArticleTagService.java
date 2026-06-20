package com.nebula.article.service;

import com.mybatisflex.core.service.IService;
import com.nebula.article.entity.ArticleTag;

import java.util.List;
import java.util.Map;

/**
 * 文章标签关联 Service
 */
public interface ArticleTagService extends IService<ArticleTag> {

    /**
     * 绑定文章标签
     */
    boolean bindTags(Long articleId, List<Long> tagIds);

    /**
     * 删除文章全部标签
     */
    Integer removeTags(Long articleId);


    /**
     * 获取标签下对应的文章数目
     * @return 标签ID，文章数目
     */
    Map<Long,Long> getArticleCountByTag();
}