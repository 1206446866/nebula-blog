package com.nebula.article.service;

import com.mybatisflex.core.service.IService;
import com.nebula.article.entity.ArticleTag;

import java.util.List;
import java.util.Map;

/** 文章与标签关联服务。 */
public interface ArticleTagService extends IService<ArticleTag> {

    /**
     * 校验并去重后替换文章标签，空列表表示清空关联。
     *
     * @param articleId 文章ID
     * @param tagIds 标签ID列表
     * @return 是否绑定成功
     */
    boolean bindTags(Long articleId, List<Long> tagIds);

    /**
     * 移除文章的全部标签关联，不删除标签本身。
     *
     * @param articleId 文章ID
     * @return 删除的关联记录数
     */
    Integer removeTags(Long articleId);


    /**
     * 统计各标签关联的有效已发布文章数量，排除草稿及已删除文章。
     *
     * @return 标签ID与有效已发布文章数的映射
     */
    Map<Long,Long> getArticleCountByTag();
}
