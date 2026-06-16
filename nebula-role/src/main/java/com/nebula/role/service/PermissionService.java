package com.nebula.role.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.role.dto.CreatePermissionDto;import com.nebula.role.dto.PermissionPageDto;
import com.nebula.role.dto.SaveRolePermissionsDto;import com.nebula.role.dto.UpdatePermissionDto;import com.nebula.role.entity.Permission;
import com.nebula.role.vo.PermissionVO;

import java.util.List;
import java.util.Map;

public interface PermissionService extends IService<Permission> {


    Map<String, List<PermissionVO>> getPermissionByRoleId(String roleId);

    /**
     * 获取用户权限
     */
    List<String> getPermissionsByUserId(Long userId);

    // 给角色分配权限
    Boolean assignPermissionsToRole(SaveRolePermissionsDto dto);

    Page<PermissionVO> getPermissionPage(PermissionPageDto dto);

    Boolean createPermission(CreatePermissionDto permission);
    Boolean updatePermission(UpdatePermissionDto permission);
    Boolean removePermission(Long id);
}