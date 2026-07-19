package com.nebula.role.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.role.dto.CreateRoleDto;
import com.nebula.role.dto.RolePageDto;
import com.nebula.role.entity.Role;
import com.nebula.role.vo.RoleVO;

import java.util.List;

public interface RoleService extends IService<Role> {

    List<RoleVO> findAll();

    /**
     * 分页查询角色，可根据角色名模糊搜索
     */
    Page<RoleVO> pageRoles(RolePageDto dto);


    /**
     * 查询用户拥有的角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> getRolesByUserId(Long userId);

    /**
     * 移除角色
     *
     * @param roleId 角色ID
     * @return 是否成功
     */
    Boolean removeRole( Long roleId);

    Boolean createRole(CreateRoleDto dto);

    List<String> getDescriptions(Long userId);
}