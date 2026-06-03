package com.nebula.auth.service.impl;

import com.nebula.auth.dto.LoginDTO;
import com.nebula.auth.entity.Permission;
import com.nebula.auth.entity.RolePermission;
import com.nebula.auth.mapper.PermissionMapper;
import com.nebula.auth.mapper.RolePermissionMapper;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.util.JwtUtil;
import com.nebula.auth.util.SecurityUtils;
import com.nebula.auth.vo.LoginVO;
import com.nebula.common.exception.AuthenticationException;
import com.nebula.common.exception.code.AuthErrorCode;
import com.nebula.user.entity.User;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.nebula.auth.entity.table.PermissionTableDef.PERMISSION;
import static com.nebula.auth.entity.table.RolePermissionTableDef.ROLE_PERMISSION;
import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;
import static com.nebula.user.entity.table.UserTableDef.USER;

/**
 * 认证 Service 实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SecurityUtils securityUtils;
    private final UserMapper userMapper;

    /**
     * 用户角色关联 UserRole
     */
    private final UserRoleMapper userRoleMapper;

    /**
     * 角色权限关联 RolePermissionMapper
     */
    private final RolePermissionMapper rolePermissionMapper;

    /**
     * 权限 PermissionMapper
     */
    private final PermissionMapper permissionMapper;

    /**
     * JWT 工具类
     */
    private final JwtUtil jwtUtil;


    /**
     * 用户登录
     */
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        User user = userMapper.selectOneByCondition(
                USER.USERNAME.eq(loginDTO.getUsername())
        );

        if (user == null) {
            throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
        }

        if (!securityUtils.matchesPassword(loginDTO.getPassword(), user.getPassword())) {
            throw new AuthenticationException(AuthErrorCode.PASSWORD_ERROR);
        }

        String token = jwtUtil.createToken(user);

        return new LoginVO(token, user);
    }

    @Override
    public List<String> getUserPermissionsByUserId(Long userId) {
        // 1. 查询用户角色
        List<Long> roleIds = userRoleMapper.selectListByCondition(USER_ROLE.USER_ID.eq(userId))
                .stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());

        if (roleIds.isEmpty()) return List.of();

        // 2. 查询角色权限
        List<Long> permissionIds = rolePermissionMapper.selectListByCondition(ROLE_PERMISSION.ROLE_ID.in(roleIds))
                .stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());

        if (permissionIds.isEmpty()) return List.of();

        // 3. 查询权限标识
        return permissionMapper.selectListByCondition(PERMISSION.ID.in(permissionIds))
                .stream()
                .map(Permission::getName)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasPermission(Long userId, String permission) {
        List<String> permissions = getUserPermissionsByUserId(userId);
        return permissions.contains(permission);
    }


}
