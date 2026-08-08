package com.nebula.auth.controller;

import com.nebula.auth.dto.*;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.vo.LoginVO;
import com.nebula.common.mail.service.MailService;
import com.nebula.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
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
    private final MailService mailService;
    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", security = {})
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }

    @PostMapping("/email/captcha")
    public Result<Long> sendEmailCaptcha(@Valid @RequestBody EmailCaptchaDTO dto) {
        return Result.success(authService.sendEmailCaptcha(dto.getEmail().trim()),"验证码已发送至"+dto.getEmail()+"若没有收到请注意是否被归为垃圾邮件");
    }

    @PostMapping("/email/verify")
    public Result<String> emailVerify(@Valid @RequestBody EmailVerifyDTO dto){
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
    @PreAuthorize("hasAuthority('user:update')")
    @PostMapping("/change-password")
    public Result<Boolean> changePassword(@RequestBody @Valid ChangePasswordDTO dto) {
        return Result.success(authService.changePassword(dto));
    }

    /**
     * 管理员重置用户密码
     */
    @PreAuthorize("hasAuthority('user:update')")
    @PostMapping("/reset-password/{userId}")
    public String resetPassword(@PathVariable Long userId) {
        boolean success = authService.resetPassword(userId);
        mailService.sendText(
                "brs1206446866@gmail.com",
                "Nebula测试邮件",
                "邮件发送成功"
        );
        return success ? Result.success("重置成功").getMessage() : Result.error("重置失败").getMessage();
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