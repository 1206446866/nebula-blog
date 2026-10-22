package com.nebula.article.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.article.entity.ArticleCategory;
import com.nebula.article.mapper.ArticleCategoryMapper;
import com.nebula.article.service.ArticleCategoryService;
import com.nebula.article.service.ArticleCatalogReferences;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.nebula.article.entity.table.ArticleCategoryTableDef.ARTICLE_CATEGORY;

@Service
@RequiredArgsConstructor
public class ArticleCategoryServiceImpl extends ServiceImpl<ArticleCategoryMapper, ArticleCategory> implements ArticleCategoryService {

    private final ArticleCatalogReferences references;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindCategories(Long articleId, Long categoryId) {
        references.lockCategory(categoryId);
        // 1 删除旧关系
        removeCategories(articleId);
        // 2 批量新增新关系
        mapper.insert(ArticleCategory.create()
                .setArticleId(articleId)
                .setCategoryId(categoryId)
        );
        return true;
    }

    @Override
    public boolean removeCategories(Long articleId) {
        // 根据 articleId 删除关系
        QueryWrapper categoryQuery = QueryWrapper.create()
                .where(ARTICLE_CATEGORY.ARTICLE_ID.eq(articleId));
        mapper.deleteByQuery(categoryQuery);
        return true;
    }
}
