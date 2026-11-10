package com.nebula.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nebula.auth.dto.*;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.vo.LoginVO;
import com.nebula.auth.service.AuthAbuseProtection;
import com.nebula.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证与账号安全", description = "登录、邮箱注册、当前会话及密码维护。")
public class AuthController {

    private final AuthService authService;
    private final AuthAbuseProtection abuseProtection;

    @Operation(summary = "获取当前登录会话",
            description = "返回当前账号资料、角色和权限，需已登录。")
    @GetMapping("/session")
    @PreAuthorize("isAuthenticated()")
    public Result<com.nebula.auth.vo.CurrentSessionVO> currentSession() {
        return Result.success(authService.currentSession());
    }
    @Operation(summary = "用户登录",
            description = "按登录类型验证凭据，返回登录结果；受来源IP和账号尝试次数限制。")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO, @Parameter(hidden = true) HttpServletRequest request) {
        abuseProtection.requireLoginIp(request);
        return Result.success(authService.login(loginDTO));
    }

    @Operation(summary = "退出当前登录会话",
            description = "撤销当前Bearer Token，退出后该Token不可继续使用。")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public Result<Boolean> logout(@Parameter(hidden = true) @RequestHeader("Authorization") String authorization) {
        authService.logout(authorization.substring(7));
        return Result.success(true);
    }

    @Operation(summary = "发送注册邮箱验证码",
            description = "向目标邮箱发送验证码，返回发送冷却秒数；受邮箱及来源IP频率限制。")
    @PostMapping("/email/captcha")
    public Result<Long> sendEmailCaptcha(@Valid @RequestBody EmailCaptchaDTO dto, @Parameter(hidden = true) HttpServletRequest request) {
        abuseProtection.requireEmailIp(request);
        return Result.success(authService.sendEmailCaptcha(dto.getEmail().trim()),"验证码已发送至"+dto.getEmail()+"若没有收到请注意是否被归为垃圾邮件");
    }

    @Operation(summary = "验证邮箱验证码",
            description = "校验验证码并发送注册确认邮件；返回确认提示，不直接完成账号注册。")
    @PostMapping("/email/verify")
    public Result<String> emailVerify(@Valid @RequestBody EmailVerifyDTO dto, @Parameter(hidden = true) HttpServletRequest request){
        abuseProtection.requireVerifyIp(request);
        return Result.success(authService.verifyEmailCaptcha(dto),"确认邮件已发送至"+dto.getEmail()+"若没有收到请注意是否被归为垃圾邮件");
    }

    @Operation(summary = "完成账号注册",
            description = "使用注册确认凭证完成注册，凭证不可重复使用。")
    @PostMapping("/register")
    public Result<Boolean> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    @Operation(summary = "修改本人密码",
            description = "验证当前密码并设置新密码，成功后撤销旧Token；新密码受服务端密码规则限制。")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public Result<Boolean> changePassword(@RequestBody @Valid ChangePasswordDTO dto) {
        return Result.success(authService.changePassword(dto));
    }

    @Operation(summary = "管理员重置用户密码",
            description = "需管理员及user:update权限，验证操作者密码并检查目标账号层级，成功后撤销目标账号旧Token。")
    @PreAuthorize("hasRole('ADMIN') && @securityUtils.hasPermission('user:update')")
    @PostMapping("/reset-password/{userId}")
    public Result<Boolean> resetPassword(@Parameter(description = "用户编号") @PathVariable @Min(1) Long userId, @Valid @RequestBody ResetPasswordDTO dto) {
        return Result.success(authService.resetPassword(userId, dto));
    }

    @Operation(summary = "查询用户权限列表",
            description = "需user:list权限，返回目标用户的权限标识列表。")
    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping("/user/permissions/{userId}")
    public Result<List<String>> getUserPermissions(@Parameter(description = "用户编号") @PathVariable Long userId) {
        return Result.success(authService.getUserPermissionsByUserId(userId));
    }

    @Operation(summary = "判断用户是否拥有权限",
            description = "需user:list权限，返回目标用户是否拥有指定权限。")
    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping("/user/has-permission/{userId}")
    public Result<Boolean> hasPermission(@Parameter(description = "用户编号") @PathVariable Long userId, @Parameter(description = "待判断的权限标识") @RequestParam String permission) {
        boolean has = authService.hasPermission(userId, permission);
        return Result.success(has);
    }
}
