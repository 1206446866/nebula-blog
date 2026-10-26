package com.nebula.role.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.role.dto.CreatePermissionDto;import com.nebula.role.dto.PermissionPageDto;
import com.nebula.role.dto.SaveRolePermissionsDto;import com.nebula.role.dto.UpdatePermissionDto;import com.nebula.role.entity.Permission;
import com.nebula.role.vo.PermissionVO;

import java.util.List;
import java.util.Map;

/** 权限目录与角色授权管理服务。 */
public interface PermissionService extends IService<Permission> {

    /**
     * 按业务前缀分组查询全部有效权限。
     *
     * @return 业务前缀与权限列表的映射
     */
    Map<String, List<PermissionVO>> getPermissions();

    /**
     * 按业务前缀查询角色权限，超级管理员返回全部有效权限。
     *
     * @param roleId 角色ID
     * @return 业务前缀与权限列表的映射
     */
    Map<String, List<PermissionVO>> getPermissionByRoleId(String roleId);

    /**
     * 合并用户各角色的有效权限，超级管理员拥有全部有效权限。
     *
     * @param userId 用户ID
     * @return 去重后的权限标识列表
     */
    List<String> getPermissionsByUserId(Long userId);

    /**
     * 由超级管理员校验权限ID后替换角色授权，不覆盖顶层角色的固有权限。
     *
     * @param dto 角色ID与完整的目标权限列表
     * @return 是否分配成功
     */
    Boolean assignPermissionsToRole(SaveRolePermissionsDto dto);

    /**
     * 按筛选条件分页查询权限。
     *
     * @param dto 分页与筛选条件
     * @return 权限分页结果
     */
    Page<PermissionVO> getPermissionPage(PermissionPageDto dto);

    /**
     * 由超级管理员创建权限。
     *
     * @param permission 权限标识及说明
     * @return 是否创建成功
     */
    Boolean createPermission(CreatePermissionDto permission);
    /**
     * 由超级管理员修改已有权限。
     *
     * @param permission 权限ID及待修改内容
     * @return 是否修改成功
     */
    Boolean updatePermission(UpdatePermissionDto permission);
    /**
     * 由超级管理员删除权限，并清理角色与该权限的关联。
     *
     * @param id 权限ID
     * @return 是否删除成功
     */
    Boolean removePermission(Long id);
}