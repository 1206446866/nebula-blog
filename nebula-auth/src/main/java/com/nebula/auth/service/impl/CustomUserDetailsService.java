package com.nebula.auth.service.impl;

import com.nebula.auth.security.AuthLoginUser;
import com.nebula.common.exception.code.AuthErrorCode;
import com.nebula.role.entity.Role;
import com.nebula.role.service.PermissionService;
import com.nebula.role.service.RoleService;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.nebula.user.entity.table.UserTableDef.USER;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    private final RoleService roleService;

    private final PermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String nid) throws UsernameNotFoundException {
        User user = userMapper.selectOneByCondition(USER.NID.eq(nid));
        if (user == null) {
            throw new UsernameNotFoundException(AuthErrorCode.USER_NOT_FOUND.getMessage());
        }
        List<Role> roleList = roleService.getRolesByUserId(user.getId());
        List<String> roles = roleList.stream().map(Role::getName).toList();
        List<String> permissions = permissionService.getPermissionsByUserId(user.getId());
        return new AuthLoginUser(user, roles, permissions);
    }

}
