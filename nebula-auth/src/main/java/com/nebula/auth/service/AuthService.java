package com.nebula.auth.service;

import com.nebula.auth.dto.LoginDTO;
import com.nebula.auth.dto.RegisterRequestDTO;
import com.nebula.auth.vo.LoginVO;

import java.util.List;

public interface AuthService {


    boolean matchesPassword(String rawPassword, String encodedPassword);
    /**
     * 用户登录
     *
     * @param loginDTO 登录参数
     * @return 登录结果
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 注册用户
     *
     * @param dto 注册信息
     * @return 操作是否成功
     */
    Boolean register(RegisterRequestDTO dto);

    /**
     * 修改用户密码（用户自己操作）
     *
     * @param userId      用户ID
     * @param newPassword 新密码
     * @return 操作是否成功
     */
    Boolean changePassword(Long userId, String newPassword);

    /**
     * 管理员重置用户密码
     *
     * @param userId 用户ID
     * @return 操作是否成功
     */
    Boolean resetPassword(Long userId);

    /**
     * 查询用户拥有的权限标识
     *
     * @param userId 用户ID
     * @return 权限标识列表，例如 ["user:create", "role:assign"]
     */
    List<String> getUserPermissionsByUserId(Long userId);

    /**
     * 判断用户是否拥有指定权限
     *
     * @param userId     用户ID
     * @param permission 权限标识
     * @return true=拥有权限, false=没有权限
     */
    boolean hasPermission(Long userId, String permission);



}
