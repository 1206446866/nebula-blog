package com.nebula.user.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户信息展示 VO
 * 用于用户主页 / 个人中心展示
 */
@Data
@Accessors(chain = true)
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 头像地址
     * 示例：/avatar/xxx.png
     */
    private String avatar;

    /**
     * 用户角色
     */
    private String role;

    /**
     * 用户状态（可选扩展）
     */
    private Integer status;

    public static UserVO create() {
        return new UserVO();
    }

}