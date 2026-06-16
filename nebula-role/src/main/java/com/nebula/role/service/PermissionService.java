package com.nebula.role.service;

import com.mybatisflex.core.service.IService;
import com.nebula.role.dto.SaveRolePermissionsDto;import com.nebula.role.entity.Permission;
import com.nebula.role.vo.PermissionVO;

import java.util.List;
import java.util.Map;

public interface PermissionService extends IService<Permission> {


    Map<String, List<PermissionVO>> getPermissionByRoleId(String roleId);

    Boolean saveRolePermissions(SaveRolePermissionsDto dto);

    // 给角色分配权限
    boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds);

    // 移除角色权限
    boolean removePermissionFromRole(Long roleId, Long permissionId);

    /**
     * 获取用户权限
     */
    List<String> getPermissionsByUserId(Long userId);
}