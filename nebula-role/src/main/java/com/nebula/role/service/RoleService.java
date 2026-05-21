package com.nebula.role.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.role.entity.Role;

import java.util.List;

public interface RoleService extends IService<Role> {

    /**
     * 分页查询角色，可根据角色名模糊搜索
     */
    Page<Role> pageRoles(int page, int size, String roleName);

    /**
     * 查询用户的所有角色
     */
    List<Role> getRolesByUserId(Long userId);

    /**
     * 给用户分配角色
     */
    boolean assignRolesToUser(Long userId, List<Long> roleIds);

    /**
     * 移除用户角色
     */
    boolean removeUserRole(Long userId, Long roleId);


}