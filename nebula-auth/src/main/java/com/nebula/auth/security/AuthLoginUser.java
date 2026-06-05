package com.nebula.auth.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nebula.common.security.LoginUser;
import com.nebula.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录用户
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginUser implements LoginUser {

    /**
     * 当前登录用户
     */
    private User user;
    /**
     * 权限标识集合
     */
    private List<String> permissions;
    /**
     * Spring Security 权限集合
     */
    @JsonIgnore
    private List<GrantedAuthority> authorities;

    public AuthLoginUser(User user, List<String> permissions) {
        this.user = user;
        this.permissions = permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities != null) {
            return authorities;
        }
        authorities = permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == 1;
    }

    @Override
    public Long getUserId() {
        return user.getId();
    }
}