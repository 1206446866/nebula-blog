package com.nebula.auth.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.nebula.auth.dto.LoginDTO;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.util.JwtUtil;
import com.nebula.user.entity.User;
import com.nebula.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.nebula.user.entity.table.UserTableDef.USER;

/**
 * 认证 Service 实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /**
     * 用户 Service
     */
    private final UserService userService;

    /**
     * JWT 工具类
     */
    private final JwtUtil jwtUtil;

    /**
     * 密码加密器
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     */
    @Override
    public String login(LoginDTO loginDTO) {

        // 查询用户
        User user = userService.getOne(QueryWrapper.create().where(USER.USERNAME.eq(loginDTO.getUsername())));

        // 用户不存在
        if (user == null) {

            throw new RuntimeException("用户不存在");
        }

        // 用户被禁用
        if (user.getStatus() != null && user.getStatus() == 0) {

            throw new RuntimeException("用户已被禁用");
        }

        // 校验密码
        boolean matches = passwordEncoder.matches(loginDTO.getPassword(), user.getPassword());

        if (!matches) {

            throw new RuntimeException("密码错误");
        }

        // 生成 JWT
        return jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
    }
}
