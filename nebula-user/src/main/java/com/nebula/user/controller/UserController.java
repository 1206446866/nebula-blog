package com.nebula.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.nebula.common.result.Result;
import com.nebula.common.util.SecurityUtils;
import com.nebula.user.dto.EditUserDTO;
import com.nebula.user.dto.CancelAccountDTO;
import com.nebula.user.dto.UpdateNameDTO;
import com.nebula.user.entity.User;
import com.nebula.user.service.UserService;
import com.nebula.user.vo.UserVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
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
@Tag(name = "用户管理", description = "用户列表、账号状态、本人资料及注销。")
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户",
            description = "需user:list权限，可按角色和用户名筛选。")
    @PreAuthorize("@securityUtils.hasPermission('user:list')")
    @GetMapping
    public Result<Page<UserVO>> page(@Parameter(description = "角色筛选条件") @RequestParam(required = false) String role, @Parameter(description = "用户名筛选条件") @RequestParam(required = false) String username, @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) Integer current, @Parameter(description = "每页数量，以当前接口校验上限为准") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size) {
        return Result.success(userService.pageUsers(role, username, current, size));
    }

    @Operation(summary = "修改用户状态",
            description = "需user:update权限，并检查目标账号权限层级及最后一名超级管理员保护。")
    @PreAuthorize("@securityUtils.hasPermission('user:update')")
    @PutMapping("/switchStatusById/{id}")
    public Result<Boolean> switchStatusById(@Parameter(description = "用户编号") @PathVariable @Min(1) Long id, @Parameter(description = "用户状态：0正常、1禁用、2锁定") @RequestParam @Min(0) @Max(2) Integer status) {
        return Result.success(userService.switchStatusById(id, status));
    }

    @Operation(summary = "编辑用户及角色",
            description = "仅超级管理员可操作，检查角色分配边界及超级管理员保护。")
    @PreAuthorize("@securityUtils.isSuperAdmin()")
    @PutMapping("/edit")
    public Result<Boolean> editUser(@RequestBody @Valid EditUserDTO editUserDTO) {
        return Result.success(userService.editUser(editUserDTO));
    }

    @Operation(summary = "删除用户",
            description = "需user:delete权限，并检查目标账号权限层级及超级管理员保护。")
    @PreAuthorize("@securityUtils.hasPermission('user:delete')")
    @DeleteMapping("/deleteUserById/{id}")
    public Result<Boolean> deleteUserById(@Parameter(description = "用户编号") @PathVariable Long id) {
        return Result.success(userService.deleteUserById(id));
    }

    @Operation(summary = "上传本人头像",
            description = "需已登录，校验真实图片及上传额度，返回头像相对资源路径。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "multipart/form-data",
                            schema = @Schema(type = "object", requiredProperties = {"file"}),
                            schemaProperties = @SchemaProperty(name = "file",
                                    schema = @Schema(type = "string", format = "binary", description = "待上传的图片文件")))))
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/upload/avatar")
    public Result<String> uploadAvatar(@Parameter(description = "待上传的图片文件，最大10MiB，需通过真实图片及像素检查") @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = SecurityUtils.getUserId();
        return Result.success(userService.uploadAvatar(userId, file));
    }

    @Operation(summary = "修改本人用户名",
            description = "需已登录，只修改当前账号的用户名。")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/self/name")
    public Result<Boolean> rename(@RequestBody @Valid UpdateNameDTO dto) {
        return Result.success(userService.rename(dto));
    }

    @Operation(summary = "注销本人账号",
            description = "需已登录，验证当前密码和注销确认，清理私人数据并撤销登录状态。")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/self")
    public Result<Boolean> cancelCurrentAccount(@RequestBody @Valid CancelAccountDTO dto) {
        return Result.success(userService.cancelCurrentAccount(dto));
    }

    @Operation(summary = "查询用户公开资料",
            description = "仅返回允许公开展示的用户资料；目标不可见或不存在时返回空数据。")
    @GetMapping("/{userId}")
    public Result<UserVO> getUserById(@Parameter(description = "用户编号") @PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return Result.success(user == null ? null : BeanUtil.copyProperties(user, UserVO.class));
    }


    @Operation(summary = "批量查询用户公开资料",
            description = "按编号批量返回允许公开展示的用户资料，不返回密码等敏感字段。")
    @GetMapping("/batch")
    public Result<List<UserVO>> getUserByIds(@Parameter(description = "用户编号列表") @RequestParam List<Long> ids) {
        return Result.success(userService.getUserByIds(ids));
    }
}
