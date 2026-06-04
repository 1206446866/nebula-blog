package com.nebula.user.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户文章
 */
@Data
@Accessors(chain = true)
public class UserArticleVO {

    /**
     * 文章ID
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 浏览量
     */
    private Long viewCount;

    public static UserArticleVO create() {
        return new UserArticleVO();
    }
}