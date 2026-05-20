package com.nebula.article.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.article.entity.ArticleCategory;
import com.nebula.article.mapper.ArticleCategoryMapper;
import com.nebula.article.service.ArticleCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleCategoryServiceImpl extends ServiceImpl<ArticleCategoryMapper, ArticleCategory> implements ArticleCategoryService {

    @Override
    public boolean bindCategories(Long articleId, List<Long> categoryIds) {

        // TODO:
        // 1 删除旧关系
        // 2 批量新增新关系

        return false;
    }

    @Override
    public boolean removeCategories(Long articleId) {

        // TODO:
        // 根据 articleId 删除关系

        return false;
    }

    @Override
    public List<Long> getCategoryIds(Long articleId) {

        // TODO:
        // 查询 categoryId 列表

        return null;
    }
}