package com.nebula.role.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.mybatisflex.core.paginate.Page;
import com.nebula.common.result.Result;
import com.nebula.role.dto.CreateRoleDto;
import com.nebula.role.dto.RolePageDto;
import com.nebula.role.dto.UpdateRoleDto;
import com.nebula.role.entity.Role;
import com.nebula.role.service.RoleService;
import com.nebula.role.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "角色查询与超级管理员维护入口。")
public class RoleController {

    private final RoleService roleService;


    @Operation(summary = "查询角色列表",
            description = "需user:list权限，返回可查询的角色列表。")
    @PreAuthorize("@securityUtils.hasPermission('user:list')")
    @GetMapping
    public Result<List<RoleVO>> getAllRoles() {
        return Result.success(roleService.findAll());
    }

    @Operation(summary = "分页查询角色",
            description = "需user:list权限，可按角色描述筛选。")
    @PreAuthorize("@securityUtils.hasPermission('user:list')")
    @GetMapping("/page")
    public Result<Page<RoleVO>> getPageRoles(@Valid RolePageDto dto) {
        return Result.success(roleService.pageRoles(dto));
    }

    @Operation(summary = "查询用户角色",
            description = "需user:list权限，返回目标用户当前关联的角色。")
    @PreAuthorize("@securityUtils.hasPermission('user:list')")
    @GetMapping("/{userId}")
    public Result<List<Role>> getRolesByUserId(@Parameter(description = "用户编号") @PathVariable Long userId) {
        List<Role> roles = roleService.getRolesByUserId(userId);
        return Result.success(roles);
    }

    @Operation(summary = "创建角色",
            description = "仅超级管理员可操作，按角色名称、描述及边界规则创建。")
    @PreAuthorize("@securityUtils.isSuperAdmin()")
    @PostMapping("/create")
    public Result<Boolean> createRole(@RequestBody @Valid CreateRoleDto dto) {
        return  Result.success(roleService.createRole(dto));
    }

    @Operation(summary = "修改角色",
            description = "仅超级管理员可操作，检查内置角色及权限层级限制。")
    @PreAuthorize("@securityUtils.isSuperAdmin()")
    @PutMapping("/update")
    public Result<Boolean> updateRole(@RequestBody @Valid UpdateRoleDto dto) {
        return Result.success(roleService.updateRole(dto));
    }

    @Operation(summary = "删除角色",
            description = "仅超级管理员可操作，检查内置角色保护并清理关联。")
    @PreAuthorize("@securityUtils.isSuperAdmin()")
    @DeleteMapping("/remove/{roleId}")
    public Result<Boolean> removeRole(@Parameter(description = "角色编号") @PathVariable Long roleId) {
        return Result.success(roleService.removeRole(roleId));
    }

}
