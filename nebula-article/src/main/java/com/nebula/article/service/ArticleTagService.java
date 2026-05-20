package com.nebula.article.service;

import com.mybatisflex.core.service.IService;
import com.nebula.article.entity.ArticleTag;

import java.util.List;

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
    boolean removeTags(Long articleId);

    /**
     * 查询文章标签ID列表
     */
    List<Long> getTagIds(Long articleId);
}