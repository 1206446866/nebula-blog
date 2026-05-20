package com.nebula.article.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.article.entity.ArticleTag;
import com.nebula.article.mapper.ArticleTagMapper;
import com.nebula.article.service.ArticleTagService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文章标签关联 Service 实现
 */
@Service
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagMapper, ArticleTag> implements ArticleTagService {

    @Override
    public boolean bindTags(Long articleId, List<Long> tagIds) {

        // TODO:
        // 1 删除旧关系
        // 2 批量新增新关系

        return false;
    }

    @Override
    public boolean removeTags(Long articleId) {

        // TODO:
        // 根据 articleId 删除标签关系

        return false;
    }

    @Override
    public List<Long> getTagIds(Long articleId) {

        // TODO:
        // 查询标签ID列表

        return null;
    }
}