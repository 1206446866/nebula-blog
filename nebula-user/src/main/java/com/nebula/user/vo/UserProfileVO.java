package com.nebula.user.vo;


import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class UserProfileVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 角色
     */
    private String role;

    /**
     * 文章数量
     */
    private Integer articleCount;

    /**
     * 评论数量
     */
    private Integer commentCount;

    /**
     * 总浏览量
     */
    private Long totalViewCount;

    /**
     * 最近发布文章
     */
    private List<UserArticleVO> recentArticles;

    public static UserProfileVO create() {
        return new UserProfileVO();
    }
}