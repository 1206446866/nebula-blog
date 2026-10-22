package com.nebula.auth.security;

import com.nebula.common.constant.UserStatus;
import com.nebula.common.constant.RoleEnum;
import com.nebula.common.security.LoginUser;
import com.nebula.user.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.List;

/**
 * 登录用户
 */
@Data
@NoArgsConstructor
public class AuthLoginUser implements LoginUser {

    /**
     * 当前登录用户
     */
    private User user;

    /**
     * 角色标识集合
     */
    private List<String> roles;

    /**
     * 权限标识集合
     */
    private List<String> permissions;

    public AuthLoginUser(User user, List<String> roles, List<String> permissions) {
        this.user = user;
        this.roles = roles;
        this.permissions = permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        var authorities = new LinkedHashSet<String>();
        if (roles != null) {
            roles.stream().filter(role -> role != null && !role.isBlank())
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .forEach(authorities::add);
        }
        if (authorities.contains("ROLE_" + RoleEnum.SUPER_ADMIN.getCode())) {
            authorities.add("ROLE_" + RoleEnum.ADMIN.getCode());
        }
        if (authorities.contains("ROLE_" + RoleEnum.ADMIN.getCode())) {
            authorities.add("ROLE_" + RoleEnum.USER.getCode());
        }
        if (permissions != null) {
            permissions.stream().filter(permission -> permission != null && !permission.isBlank())
                    .forEach(authorities::add);
        }
        return authorities.stream().map(SimpleGrantedAuthority::new).toList();
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
        return !Integer.valueOf(1).equals(user.getDeleted()) && UserStatus.NORMAL.matches(user.getStatus());
    }

    @Override
    public Long getUserId() {
        return user.getId();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !UserStatus.LOCKED.matches(user.getStatus());
    }

    public static AuthLoginUser create() {
        return new AuthLoginUser();
    }
}
