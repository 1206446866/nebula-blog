package com.nebula.auth;

import com.nebula.auth.security.AuthLoginUser;
import com.nebula.role.entity.Role;
import com.nebula.role.service.PermissionService;
import com.nebula.role.service.RoleService;
import com.nebula.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthUserBuilder {

    private final RoleService roleService;
    private final PermissionService permissionService;


    public AuthLoginUser createLoginUser(User user) {

        List<Role> roleList =
                roleService.getRolesByUserId(user.getId());

        List<String> roles =
                roleList.stream()
                        .map(Role::getName)
                        .toList();

        List<String> permissions =
                permissionService.getPermissionsByUserId(user.getId());

        return new AuthLoginUser(
                user,
                roles,
                permissions
        );
    }
}
