package com.nebula.user.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.nebula.common.result.Result;
import com.nebula.common.util.SecurityUtils;
import com.nebula.user.dto.EditUserDTO;
import com.nebula.user.dto.UpdateNameDTO;
import com.nebula.user.entity.User;
import com.nebula.user.service.UserService;
import com.nebula.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Validated
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 查询用户列表（可分页/可模糊查询用户名）
     */
    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping
    public Result<Page<UserVO>> page(@RequestParam(required = false) String role, @RequestParam(required = false) String username, @RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "10") Integer size) {
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
    public Result<Boolean> editUser(@RequestBody @Valid EditUserDTO editUserDTO) {
        return Result.success(userService.editUser(editUserDTO));
    }

    /**
     * 删除用户
     * @param id 用户ID
     */
    @PreAuthorize("hasAuthority('user:update')")
    @DeleteMapping("/deleteUserById/{id}")
    public Result<Boolean> deleteUserById(@PathVariable Long id) {
        return Result.success(userService.deleteUserById(id));
    }

    /**
     * 上传用户头像
     *
     * @param file 文件
     * @return 头像URL
     */
    @PreAuthorize("hasAuthority('user:update')")
    @PostMapping("/upload/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        Long userId = SecurityUtils.getUserId();
        return Result.success(userService.uploadAvatar(userId, file));
    }

    @PreAuthorize("hasAuthority('user:update')")
    @PutMapping("/self/name")
    public Result<Boolean> rename(@RequestBody @Valid UpdateNameDTO dto) {
        return Result.success(userService.rename(dto));
    }

    @GetMapping("/{userId}")
    public Result<UserVO> getUserById(@PathVariable Long userId) {
        return Result.success(BeanUtil.copyProperties(User.create().where(User::getId).eq(userId).one(), UserVO.class));
    }


    @GetMapping("/batch")
    public Result<List<UserVO>> getUserByIds(@RequestParam List<Long> ids) {
        return Result.success(userService.getUserByIds(ids));
    }
}