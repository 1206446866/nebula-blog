package com.nebula.article.service;

import com.mybatisflex.core.service.IService;
import com.nebula.article.entity.ArticleCategory;

public interface ArticleCategoryService extends IService<ArticleCategory> {

    /**
     * 给文章绑定分类
     */
    boolean bindCategories(Long articleId, Long categoryIds);

    /**
     * 删除文章所有分类
     */
    boolean removeCategories(Long articleId);

}