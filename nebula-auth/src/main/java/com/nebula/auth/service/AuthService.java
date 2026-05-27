package com.nebula.auth.service;

import com.nebula.auth.dto.LoginDTO;
import com.nebula.auth.vo.LoginVO;

import java.util.List;

public interface AuthService {

    /**
     * 用户登录
     *
     * @param loginDTO 登录参数
     * @return 登录结果
     */
    LoginVO login(LoginDTO loginDTO);


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
