package com.nebula.role.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.role.dto.CreateRoleDto;
import com.nebula.role.dto.RolePageDto;
import com.nebula.role.dto.UpdateRoleDto;
import com.nebula.role.entity.Role;
import com.nebula.role.vo.RoleVO;

import java.util.List;

/** 角色查询、管理与用户角色信息服务。 */
public interface RoleService extends IService<Role> {

    /**
     * 查询全部未删除的角色。
     *
     * @return 角色展示列表
     */
    List<RoleVO> findAll();

    /**
     * 按角色说明筛选并分页查询有效角色。
     *
     * @param dto 角色说明关键词与分页条件
     * @return 角色分页结果
     */
    Page<RoleVO> pageRoles(RolePageDto dto);


    /**
     * 查询用户直接绑定且未删除的角色。
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> getRolesByUserId(Long userId);

    /**
     * 由超级管理员删除自定义角色，并清理用户和权限关联。
     *
     * @param roleId 角色ID，内置角色不允许删除
     * @return 是否删除成功
     */
    Boolean removeRole( Long roleId);

    /**
     * 由超级管理员创建角色，角色编码不能重复。
     *
     * @param dto 角色编码及说明
     * @return 是否创建成功
     */
    Boolean createRole(CreateRoleDto dto);

    /**
     * 由超级管理员修改角色，内置角色只允许修改说明。
     *
     * @param dto 角色ID、编码及说明
     * @return 是否修改成功
     */
    Boolean updateRole(UpdateRoleDto dto);

    /**
     * 查询用户直接绑定角色的说明文本。
     *
     * @param userId 用户ID
     * @return 角色说明列表
     */
    List<String> getDescriptions(Long userId);
}
