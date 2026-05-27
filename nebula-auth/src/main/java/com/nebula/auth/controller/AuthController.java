package com.nebula.auth.controller;

import com.nebula.auth.annotation.RequiresPermission;
import com.nebula.auth.dto.LoginDTO;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.util.JwtUtil;
import com.nebula.auth.vo.LoginVO;
import com.nebula.common.result.Result;
import com.nebula.user.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证 Controller
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO login = authService.login(loginDTO);
        return Result.success(login);
    }


    /**
     * 获取用户权限列表
     */
    @RequiresPermission("user:assign-role")
    @GetMapping("/user/permissions/{userId}")
    public Result<List<String>> getUserPermissions(@PathVariable Long userId) {
        return Result.success(authService.getUserPermissionsByUserId(userId));
    }

    /**
     * 判断用户是否拥有指定权限
     */
    @GetMapping("/user/has-permission/{userId}")
    public Result<Boolean> hasPermission(@PathVariable Long userId, @RequestParam String permission) {
        boolean has = authService.hasPermission(userId, permission);
        return Result.success(has);
    }
}