package com.nebula.article.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 文章标签关联表
 */
@Data(staticConstructor = "create")
@Table("article_tag")
@Accessors(chain = true) // 关键，开启链式
public class ArticleTag {

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 标签ID
     */
    private Long tagId;
}