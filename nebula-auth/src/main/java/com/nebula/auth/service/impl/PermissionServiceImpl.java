package com.nebula.auth.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.auth.entity.Permission;
import com.nebula.auth.entity.RolePermission;
import com.nebula.auth.mapper.PermissionMapper;
import com.nebula.auth.mapper.RolePermissionMapper;
import com.nebula.auth.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.nebula.auth.entity.table.PermissionTableDef.PERMISSION;
import static com.nebula.auth.entity.table.RolePermissionTableDef.ROLE_PERMISSION;
import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        List<RolePermission> rolePermissions = rolePermissionMapper.selectListByCondition(ROLE_PERMISSION.ROLE_ID.eq(roleId));
        if (rolePermissions.isEmpty()) return List.of();

        List<Long> permissionIds = rolePermissions.stream().map(RolePermission::getPermissionId).collect(Collectors.toList());

        return Permission.create().where(Permission::getId).in(permissionIds).list();
    }

    @Override
    public boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        List<RolePermission> toInsert = permissionIds.stream().map(pid -> RolePermission.create().setRoleId(roleId).setPermissionId(pid)).toList();

        int inserted = rolePermissionMapper.insertBatch(toInsert);
        return inserted > 0;
    }

    @Override
    public boolean removePermissionFromRole(Long roleId, Long permissionId) {
        var deleted = rolePermissionMapper.deleteByCondition(ROLE_PERMISSION.ROLE_ID.eq(roleId).and(ROLE_PERMISSION.PERMISSION_ID.eq(permissionId)));
        return deleted > 0;
    }

    @Override
    public List<String> getPermissionsByUserId(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(PERMISSION.NAME)
                .from(PERMISSION)
                .innerJoin(ROLE_PERMISSION)
                .on(PERMISSION.ID.eq(ROLE_PERMISSION.PERMISSION_ID))
                .innerJoin(USER_ROLE)
                .on(ROLE_PERMISSION.ROLE_ID.eq(USER_ROLE.ROLE_ID))
                .where(USER_ROLE.USER_ID.eq(userId));
        List<Permission> permissions = permissionMapper.selectListByQuery(queryWrapper);
        return permissions.stream().map(Permission::getName).distinct().toList();
    }
}