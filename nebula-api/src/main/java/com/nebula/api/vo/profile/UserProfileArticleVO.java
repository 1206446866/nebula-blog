package com.nebula.api.vo.profile;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户文章
 */
@Data
@Accessors(chain = true)
public class UserProfileArticleVO {

    /**
     * 文章ID
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 浏览量
     */
    private Long viewCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 获赞数
     */
    private Long likes = 99999999L;

    /**
     * 评论数
     */
    private Long comments = 99999999L;

    public static UserProfileArticleVO create() {
        return new UserProfileArticleVO();
    }
}