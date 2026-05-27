package com.nebula.auth.vo;

import com.nebula.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 登录响应VO
 */
@Data
@AllArgsConstructor
@Accessors(chain=true)
public class LoginVO {

    /**
     * JWT Token
     */
    private String token;

    /**
     * 当前登录用户
     */
    private User user;
}