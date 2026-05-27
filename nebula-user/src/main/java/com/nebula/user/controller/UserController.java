package com.nebula.user.controller;

import com.mybatisflex.core.paginate.Page;
import com.nebula.common.result.Result;
import com.nebula.user.entity.User;
import com.nebula.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/auth")
    public Result auth() {
        return Result.success("test");
    }

    /**
     * 查询用户列表（可分页/可模糊查询用户名）
     */
    @GetMapping("/list")
    public Result<List<User>> list(@RequestParam(required = false) String role) {
        return Result.success(userService.getUsersByRole(role));
    }

    @GetMapping("/page")
    public Result<Page<User>> page(@RequestParam int page, @RequestParam int size, @RequestParam(required = false) String role, @RequestParam(required = false) String username) {
        return Result.success(userService.pageUsers(role, username,page, size));
    }

    /**
     * 修改密码（用户自己操作）
     */
    @PostMapping("/change-password/{userId}")
    public String changePassword(@PathVariable Long userId, @RequestParam String newPassword) {
        boolean success = userService.changePassword(userId, newPassword);
        return success ? Result.success("修改成功").getMessage() : Result.error("修改失败").getMessage();
    }

    /**
     * 管理员重置用户密码
     */
    @PostMapping("/reset-password/{userId}")
    public String resetPassword(@PathVariable Long userId) {
        boolean success = userService.resetPassword(userId);
        return success ? Result.success("重置成功").getMessage() : Result.error("重置失败").getMessage();
    }

}