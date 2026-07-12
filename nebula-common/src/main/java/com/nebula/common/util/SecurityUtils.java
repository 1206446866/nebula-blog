package com.nebula.common.util;

import com.nebula.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Security 工具类
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {


    /**
     * 获取当前登录用户
     */
    public static LoginUser getLoginUser() {

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
    public static Long getUserId() {
        return Objects.requireNonNull(getLoginUser()).getUserId();
    }

    /**
     * 判断角色
     */
    public static boolean hasRole(String role) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream().anyMatch(item -> item.getAuthority().equals(authority));
    }

    /**
     * 判断权限
     */
    public static boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(item -> item.getAuthority().equals(permission));
    }
}