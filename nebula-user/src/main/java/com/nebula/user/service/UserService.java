package com.nebula.user.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.nebula.user.entity.User;

import java.util.List;


public interface UserService extends IService<User> {

    List<User> getUsers(String role, String username, int page, int size);

    long countUsers(String role, String username);

    // 根据角色查询用户
    List<User> getUsersByRole(String role);

    boolean addUser(User user);
    boolean updateUser(User user);

    /**
     * 分页查询用户，可根据角色和用户名模糊搜索
     *
     * @param page 当前页
     * @param size 每页数量
     * @param role 角色，可选
     * @param username 用户名，可选
     * @return 用户分页列表
     */
    Page<User> pageUsers(int page, int size, String role, String username);



}