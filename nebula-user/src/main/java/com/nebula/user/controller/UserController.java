package com.nebula.user.controller;

import com.mybatisflex.core.paginate.Page;
import com.nebula.common.result.Result;
import com.nebula.common.util.SecurityUtils;
import com.nebula.user.dto.EditUserDTO;
import com.nebula.user.service.UserService;
import com.nebula.user.vo.UserProfileVO;
import com.nebula.user.vo.UserVO;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        Long userId = SecurityUtils.getUserId();
        return Result.success(userService.getUserInfo(userId));
    }

    /**
     * 查询用户列表（可分页/可模糊查询用户名）
     */
    @GetMapping
    public Result<Page<UserVO>> list(@RequestParam(required = false) String role, @RequestParam(required = false) String username, @RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(userService.pageUsers(role, username, current, size));
    }

    /**
     * 切换状态
     *
     * @param id     用户ID
     * @param status 状态标识
     * @return 操作状态
     */
    @PreAuthorize("hasAuthority('user:update')")
    @PutMapping("/switchStatusById/{id}")
    public Result<Boolean> switchStatusById(@PathVariable Long id, @RequestParam Integer status) {
        return Result.success(userService.switchStatusById(id, status));
    }

    @PreAuthorize("hasAuthority('user:update')")
    @PutMapping("/edit")
    public Result<Boolean> editUser(@RequestBody EditUserDTO editUserDTO) {
        return Result.success(userService.editUser(editUserDTO));
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    @PreAuthorize("hasAuthority('user:update')")
    @DeleteMapping("/deleteUserById/{id}")
    public Result<Boolean> deleteUserById(@PathVariable Long id) {
        return Result.success(userService.deleteUserById(id));
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
        Long userId = SecurityUtils.getUserId();
        return Result.success(userService.uploadAvatar(userId, file));
    }
}