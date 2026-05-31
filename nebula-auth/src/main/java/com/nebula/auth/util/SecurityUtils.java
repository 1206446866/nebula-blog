package com.nebula.auth.util;

import com.nebula.auth.security.LoginUser;
import com.nebula.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Security 工具类
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final PasswordEncoder passwordEncoder;

    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

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
     * 获取当前用户
     */
    public User getUser() {
        LoginUser loginUser = getLoginUser();
        return loginUser == null ? null : loginUser.getUser();
    }

    /**
     * 获取当前用户ID
     */
    public Long getUserId() {
        User user = getUser();
        return user == null ? null : user.getId();
    }
}