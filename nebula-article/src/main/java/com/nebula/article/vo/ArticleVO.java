package com.nebula.article.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

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
     * 作者头像
     */
    private String authorAvatar;

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

    private Integer status;

    private Long categoryId;

    private List<Long> tagIds;


    public static ArticleVO create() {
        return new ArticleVO();
    }
}
