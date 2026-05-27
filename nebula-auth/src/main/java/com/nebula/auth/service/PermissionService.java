package com.nebula.auth.service;

import com.mybatisflex.core.service.IService;
import com.nebula.auth.entity.Permission;

import java.util.List;

public interface PermissionService extends IService<Permission> {

    // 查询角色的权限列表
    List<Permission> getPermissionsByRoleId(Long roleId);

    // 给角色分配权限
    boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds);

    // 移除角色权限
    boolean removePermissionFromRole(Long roleId, Long permissionId);

    /**
     * 获取用户权限
     */
    List<String> getPermissionsByUserId(Long userId);
}