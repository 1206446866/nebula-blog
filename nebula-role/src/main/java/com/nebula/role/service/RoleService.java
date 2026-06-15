package com.nebula.role.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.role.entity.Role;
import com.nebula.role.vo.RoleVO;
import com.nebula.user.entity.User;

import java.util.List;

public interface RoleService extends IService<Role> {

    List<RoleVO> findAll();
    /**
     * 分页查询角色，可根据角色名模糊搜索
     */
    Page<Role> pageRoles(int page, int size, String roleName);

    /**
     * 查询所有角色
     */
    List<Role> getAllRoles();

    /**
     * 查询用户拥有的角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> getRolesByUserId(Long userId);

    /**
     * 查询用户拥有的角色
     *
     * @param Nid 用户ID
     * @return 角色列表
     */
    List<Role> getRolesByUserNid(String Nid);

    /**
     * 查询角色下的所有用户
     *
     * @param roleId 角色ID
     * @return 用户列表
     */
    List<User> getUsersByRoleId(Long roleId);

    /**
     * 给用户分配角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 是否成功
     */
    boolean assignRoles(Long userId, List<Long> roleIds);

    /**
     * 移除用户角色
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 是否成功
     */
    boolean removeRole(Long userId, Long roleId);

}