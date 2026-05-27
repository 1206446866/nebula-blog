package com.nebula.user.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.user.entity.User;

import java.util.List;


public interface UserService extends IService<User> {

    /**
     * 分页查询用户，可根据角色和用户名模糊搜索
     *
     * @param page 当前页
     * @param size 每页数量
     * @param role 角色，可选
     * @param username 用户名，可选
     * @return 用户分页列表
     */
    Page<User> pageUsers(String role, String username, int page, int size);

    long countUsers(String role, String username);

    // 根据角色查询用户
    List<User> getUsersByRole(String role);

    boolean addUser(User user);
    boolean updateUser(User user);

    /**
     * 修改用户状态（启用/禁用）
     * @param userId 用户ID
     * @param status 新状态，1=启用, 0=禁用
     * @return 操作是否成功
     */
    boolean updateUserStatus(Long userId, Integer status);
    /**
     * 修改用户密码（用户自己操作）
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 操作是否成功
     */
    boolean changePassword(Long userId, String newPassword);

    /**
     * 管理员重置用户密码
     * @param userId 用户ID
     * @return 操作是否成功
     */
    boolean resetPassword(Long userId);



    /**
     * 查询所有用户
     */
    List<User> getAllUsers();

}