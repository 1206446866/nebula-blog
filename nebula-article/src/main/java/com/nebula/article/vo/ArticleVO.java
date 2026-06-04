package com.nebula.article.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class ArticleVO {

    /**
     * 文章ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 作者
     */
    private String author;

    /**
     * 评论数量
     */
    private Long commentCount;

    /**
     * 发布时间
     */
    private LocalDateTime createTime;

    /**
     * 浏览量
     */
    private Long viewCount;

    public static ArticleVO create() {
        return new ArticleVO();
    }
}
