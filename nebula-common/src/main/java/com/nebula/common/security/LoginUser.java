package com.nebula.common.security;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * 登录用户接口
 */
public interface LoginUser extends UserDetails {

    /**
     * 用户ID
     */
    Long getUserId();

}
