package com.nebula.article.service;

import com.mybatisflex.core.service.IService;
import com.nebula.article.entity.ArticleCategory;

/** 文章与分类关联服务。 */
public interface ArticleCategoryService extends IService<ArticleCategory> {

    /**
     * 校验分类后替换文章的分类关联，不删除分类本身。
     *
     * @param articleId 文章ID
     * @param categoryIds 目标分类ID
     * @return 是否绑定成功
     */
    boolean bindCategories(Long articleId, Long categoryIds);

    /**
     * 移除文章的全部分类关联，不删除分类本身。
     *
     * @param articleId 文章ID
     * @return 是否移除成功
     */
    boolean removeCategories(Long articleId);

}