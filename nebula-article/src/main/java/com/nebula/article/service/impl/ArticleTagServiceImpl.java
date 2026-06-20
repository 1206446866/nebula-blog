package com.nebula.article.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.article.entity.ArticleTag;
import com.nebula.article.mapper.ArticleTagMapper;
import com.nebula.article.service.ArticleTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.nebula.article.entity.table.ArticleTagTableDef.ARTICLE_TAG;

/**
 * 文章标签关联 Service 实现
 */
@Service
@RequiredArgsConstructor
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagMapper, ArticleTag> implements ArticleTagService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindTags(Long articleId, List<Long> tagIds) {
        // 标签重建
        // 1 删除旧关系
        removeTags(articleId);
        if (CollUtil.isEmpty(tagIds)) return true;
        // 2 批量新增新关系
        List<ArticleTag> articleTags = tagIds.stream().map(tagId -> ArticleTag.create().setArticleId(articleId).setTagId(tagId)).toList();
        getMapper().insertBatchSelective(articleTags);
        return true;
    }

    @Override
    public Integer removeTags(Long articleId) {
        // 根据 articleId 删除标签关系
        return getMapper().deleteByCondition(ARTICLE_TAG.ARTICLE_ID.eq(articleId));
    }

    @Override
    public Map<Long, Long> getArticleCountByTag() {
        QueryWrapper query = QueryWrapper.create().select(ARTICLE_TAG.TAG_ID, QueryMethods.count(ARTICLE_TAG.ARTICLE_ID).as("articleId")).groupBy(ARTICLE_TAG.TAG_ID);
        List<ArticleTag> articleTags = getMapper().selectListByQuery(query);
        return articleTags.stream().collect(Collectors.toMap(ArticleTag::getTagId, ArticleTag::getArticleId));
    }
}