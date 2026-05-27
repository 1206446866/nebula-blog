package com.nebula.role.controller;

import com.nebula.common.result.Result;
import com.nebula.role.entity.Role;
import com.nebula.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 查询用户的所有角色
     */
    @GetMapping("/user/{userId}")
    public Result<List<Role>> getRolesByUserId(@PathVariable Long userId) {
        List<Role> roles = roleService.getRolesByUserId(userId);
        return Result.success(roles);
    }

    /**
     * 给用户分配角色
     */
    @PostMapping("/user/{userId}/assign")
    public String assignRolesToUser(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        boolean success = roleService.assignRoles(userId, roleIds);
        return success ? Result.success("分配角色成功").getMessage() : Result.error("分配角色失败").getMessage();
    }

    /**
     * 移除用户角色
     */
    @DeleteMapping("/user/{userId}/remove/{roleId}")
    public String removeRoleFromUser(@PathVariable Long userId, @PathVariable Long roleId) {
        boolean success = roleService.removeRole(userId, roleId);
        return success ? Result.success("移除角色成功").getMessage() : Result.error("移除角色失败").getMessage();
    }


}