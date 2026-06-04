package com.nebula.user.controller;

import com.mybatisflex.core.paginate.Page;
import com.nebula.common.result.Result;
import com.nebula.user.entity.User;
import com.nebula.user.service.UserService;
import com.nebula.user.vo.UserProfileVO;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 查询用户列表（可分页/可模糊查询用户名）
     */
    @GetMapping("/list")
    public Result<Page<User>> list(@RequestParam(required = false) String role, @RequestParam(defaultValue = "1") Long current, @RequestParam(defaultValue = "10") Long size) {
        return Result.success(userService.getUsersByRole(role, current, size));
    }

    @GetMapping("/page")
    public Result<Page<User>> page(@RequestParam(required = false) String role, @RequestParam(required = false) String username, @RequestParam int page, @RequestParam int size) {
        return Result.success(userService.pageUsers(role, username, page, size));
    }

    @PutMapping("/switchStatusById/{id}")
    public Result<Boolean> switchStatusById(@PathVariable Long id, @RequestParam Integer status) {
        return Result.success(userService.switchStatusById(id, status));
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    @DeleteMapping("/deleteUserById/{id}")
    public Result<Boolean> deleteUserById(@PathVariable Long id) {
        return Result.success(userService.deleteUserById(id));
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

    /**
     * 获取用户主页信息
     *
     * @param id 用户ID
     * @return 用户主页信息
     */
    @GetMapping("/profile/{id}")
    public Result<UserProfileVO> getProfile(@PathVariable @Min(value = 1, message = "用户ID非法") Long id) {
        return Result.success(userService.getProfile(id));
    }

    /**
     * 上传用户头像
     *
     * @param file 文件
     * @return 头像URL
     */
    @PostMapping("/upload/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(userService.uploadAvatar(file));
    }
}