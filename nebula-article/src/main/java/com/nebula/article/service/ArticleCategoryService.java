package com.nebula.article.service;

import com.mybatisflex.core.service.IService;
import com.nebula.article.entity.ArticleCategory;

import java.util.List;

public interface ArticleCategoryService extends IService<ArticleCategory> {

    /**
     * 给文章绑定分类
     */
    boolean bindCategories(Long articleId, List<Long> categoryIds);

    /**
     * 删除文章所有分类
     */
    boolean removeCategories(Long articleId);

    /**
     * 查询文章分类ID列表
     */
    List<Long> getCategoryIds(Long articleId);
}