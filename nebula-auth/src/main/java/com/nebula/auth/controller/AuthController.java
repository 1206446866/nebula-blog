package com.nebula.auth.controller;

import com.nebula.auth.dto.*;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.vo.LoginVO;
import com.nebula.auth.service.AuthAbuseProtection;
import com.nebula.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final AuthAbuseProtection abuseProtection;

    @GetMapping("/session")
    @PreAuthorize("isAuthenticated()")
    public Result<com.nebula.auth.vo.CurrentSessionVO> currentSession() {
        return Result.success(authService.currentSession());
    }
    /**
     * 用户登录
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        abuseProtection.requireLoginIp(request);
        return Result.success(authService.login(loginDTO));
    }

    @Operation(summary = "退出当前登录会话")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public Result<Boolean> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization.substring(7));
        return Result.success(true);
    }

    @PostMapping("/email/captcha")
    public Result<Long> sendEmailCaptcha(@Valid @RequestBody EmailCaptchaDTO dto, HttpServletRequest request) {
        abuseProtection.requireEmailIp(request);
        return Result.success(authService.sendEmailCaptcha(dto.getEmail().trim()),"验证码已发送至"+dto.getEmail()+"若没有收到请注意是否被归为垃圾邮件");
    }

    @PostMapping("/email/verify")
    public Result<String> emailVerify(@Valid @RequestBody EmailVerifyDTO dto, HttpServletRequest request){
        abuseProtection.requireVerifyIp(request);
        return Result.success(authService.verifyEmailCaptcha(dto),"确认邮件已发送至"+dto.getEmail()+"若没有收到请注意是否被归为垃圾邮件");
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result<Boolean> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    /**
     * 修改密码（用户自己操作）
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public Result<Boolean> changePassword(@RequestBody @Valid ChangePasswordDTO dto) {
        return Result.success(authService.changePassword(dto));
    }

    /**
     * 管理员重置用户密码
     */
    @PreAuthorize("hasRole('ADMIN') && @securityUtils.hasPermission('user:update')")
    @PostMapping("/reset-password/{userId}")
    public Result<Boolean> resetPassword(@PathVariable @Min(1) Long userId, @Valid @RequestBody ResetPasswordDTO dto) {
        return Result.success(authService.resetPassword(userId, dto));
    }

    /**
     * 获取用户权限列表
     */
    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping("/user/permissions/{userId}")
    public Result<List<String>> getUserPermissions(@PathVariable Long userId) {
        return Result.success(authService.getUserPermissionsByUserId(userId));
    }

    /**
     * 判断用户是否拥有指定权限
     */
    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping("/user/has-permission/{userId}")
    public Result<Boolean> hasPermission(@PathVariable Long userId, @RequestParam String permission) {
        boolean has = authService.hasPermission(userId, permission);
        return Result.success(has);
    }
}
