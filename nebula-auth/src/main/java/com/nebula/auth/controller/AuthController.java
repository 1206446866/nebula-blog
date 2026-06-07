package com.nebula.auth.controller;

import com.nebula.auth.dto.LoginDTO;
import com.nebula.auth.dto.RegisterRequestDTO;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.vo.LoginVO;
import com.nebula.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }

    /**
     * 注册
     * @param dto 用户名 密码
     */
    @PostMapping("/register")
    public Result<Boolean> register(@Valid @RequestBody RegisterRequestDTO dto) {
        return Result.success(authService.register(dto));
    }

    /**
     * 修改密码（用户自己操作）
     */
    @PostMapping("/change-password/{userId}")
    public String changePassword(@PathVariable Long userId, @RequestParam String newPassword) {
        boolean success = authService.changePassword(userId, newPassword);
        return success ? Result.success("修改成功").getMessage() : Result.error("修改失败").getMessage();
    }

    /**
     * 管理员重置用户密码
     */
    @PostMapping("/reset-password/{userId}")
    public String resetPassword(@PathVariable Long userId) {
        boolean success = authService.resetPassword(userId);
        return success ? Result.success("重置成功").getMessage() : Result.error("重置失败").getMessage();
    }

    /**
     * 获取用户权限列表
     */
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