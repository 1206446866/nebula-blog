package com.nebula.auth.service;

import com.nebula.auth.dto.LoginDTO;

public interface AuthService {

    /**
     * 用户登录
     */
    String login(LoginDTO loginDTO);
}
