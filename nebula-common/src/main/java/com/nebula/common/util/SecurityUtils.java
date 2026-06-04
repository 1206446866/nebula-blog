package com.nebula.common.util;

import com.nebula.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Security 工具类
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {


    /**
     * 获取当前登录用户
     */
    public LoginUser getLoginUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }

        return null;
    }

    /**
     * 获取当前用户ID
     */
    public Long getUserId() {
        return getLoginUser().getUserId();
    }
}