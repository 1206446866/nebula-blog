package com.nebula.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.nebula.auth.dto.ChangePasswordDTO;
import com.nebula.auth.dto.LoginDTO;
import com.nebula.auth.dto.RegisterRequestDTO;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.util.JwtUtil;
import com.nebula.auth.vo.LoginVO;
import com.nebula.common.constant.RoleEnum;
import com.nebula.common.exception.AuthenticationException;
import com.nebula.common.exception.code.AuthErrorCode;
import com.nebula.common.security.LoginUser;
import com.nebula.common.util.SecurityUtils;
import com.nebula.role.entity.Permission;
import com.nebula.role.entity.Role;
import com.nebula.role.entity.RolePermission;
import com.nebula.role.mapper.PermissionMapper;
import com.nebula.role.mapper.RolePermissionMapper;
import com.nebula.role.service.PermissionService;
import com.nebula.role.service.RoleService;
import com.nebula.user.entity.User;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import com.nebula.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.nebula.role.entity.table.PermissionTableDef.PERMISSION;
import static com.nebula.role.entity.table.RolePermissionTableDef.ROLE_PERMISSION;
import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;
import static com.nebula.user.entity.table.UserTableDef.USER;

/**
 * 认证 Service 实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /**
     * 用户 User
     */
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

    private final RoleService roleService;

    private final PermissionService permissionService;

    /**
     * JWT 工具类
     */
    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public Boolean changePassword(ChangePasswordDTO dto) {
        LoginUser lu =  SecurityUtils.getLoginUser();
        if (Objects.isNull(lu) || Objects.isNull(lu.getPassword())) {
            throw new RuntimeException("当前用户状态异常");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), lu.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("两次密码不一致");
        }
        User user = User.create();
        user.setId(lu.getUserId());
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        return userMapper.update(user)>0;
    }

    //TODO
    @Override
    public Boolean resetPassword(Long userId) {
        return false;
    }

    /**
     * 用户登录
     */
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        User user = userMapper.selectOneByCondition(USER.NID.eq(loginDTO.getNid()));
        if (user == null) {
            throw new AuthenticationException(AuthErrorCode.USER_NOT_FOUND);
        }
        if (!matchesPassword(loginDTO.getPassword(), user.getPassword())) {
            throw new AuthenticationException(AuthErrorCode.PASSWORD_ERROR);
        }
        String token = jwtUtil.createToken(user);
        List<Role> roleList = roleService.getRolesByUserId(user.getId());
        List<String> roles = roleList
                .stream().map(Role::getName).toList();
        List<String> permissions = permissionService.getPermissionsByUserId(user.getId());
        return LoginVO.create().setToken(token)
                .setUser(BeanUtil.copyProperties(user, UserVO.class))
                .setRoles(roles)
                .setPermissions(permissions);
    }

    @Override
    public Boolean register(RegisterRequestDTO dto) {
//        用户创建
        User user = User.create()
                .setNid(UUID.randomUUID().toString().replace("-", ""))
                .setUsername("普通用户" + UUID.randomUUID())
                .setPassword(passwordEncoder.encode(dto.getPassword()))
                ;
//        角色关联
        userMapper.insertSelective(user);
        UserRole userRole = UserRole.create()
                .setUserId(user.getId())
                .setRoleId(RoleEnum.USER.getRoleId());
        return userRoleMapper.insertSelective(userRole) > 0;
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
