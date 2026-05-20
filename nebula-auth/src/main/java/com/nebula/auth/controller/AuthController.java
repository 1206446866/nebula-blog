package com.nebula.auth.controller;

import com.nebula.auth.dto.LoginDTO;
import com.nebula.auth.service.AuthService;
import com.nebula.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public Result<String> login(@Valid @RequestBody LoginDTO loginDTO) {

        String token = authService.login(loginDTO);

        return Result.success(token);
    }
}