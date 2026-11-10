package com.nebula.role.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.mybatisflex.core.paginate.Page;
import com.nebula.common.result.Result;
import com.nebula.role.dto.CreatePermissionDto;
import com.nebula.role.dto.PermissionPageDto;
import com.nebula.role.dto.SaveRolePermissionsDto;
import com.nebula.role.dto.UpdatePermissionDto;
import com.nebula.role.service.PermissionService;
import com.nebula.role.vo.PermissionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@Tag(name = "权限管理", description = "权限分组、分页查询与角色授权。")
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "查询分组权限列表",
            description = "需user:list权限，按业务前缀分组返回；兼容的非标准前缀归入其它分组。")
    @PreAuthorize("@securityUtils.hasPermission('user:list')")
    @GetMapping
    public Result<Map<String, List<PermissionVO>>> getPermissions() {
        return Result.success(permissionService.getPermissions());
    }

    @Operation(summary = "查询角色分组权限",
            description = "需user:list权限，返回目标角色的权限分组。")
    @PreAuthorize("@securityUtils.hasPermission('user:list')")
    @GetMapping("/{roleId}")
    public Result<Map<String, List<PermissionVO>>> getRolePermissions(@Parameter(description = "角色编号") @PathVariable String roleId) {
        return Result.success(permissionService.getPermissionByRoleId(roleId));
    }

    @Operation(summary = "保存角色权限",
            description = "仅超级管理员可操作，检查目标角色及授权边界后保存关联。")
    @PreAuthorize("@securityUtils.isSuperAdmin()")
    @PostMapping("/save")
    public Result<Boolean> saveRolePermissions(@RequestBody @Valid SaveRolePermissionsDto dto) {
        return Result.success(permissionService.assignPermissionsToRole(dto));
    }

    @Operation(summary = "分页查询权限",
            description = "需user:list权限，可按权限名称筛选。")
    @PreAuthorize("@securityUtils.hasPermission('user:list')")
    @GetMapping("/page")
    public Result<Page<PermissionVO>> getPermissionsPage(@Valid PermissionPageDto dto) {
        return Result.success(permissionService.getPermissionPage(dto));
    }

    @Operation(summary = "创建权限",
            description = "仅超级管理员可操作，检查权限标识等业务规则。")
    @PreAuthorize("@securityUtils.isSuperAdmin()")
    @PostMapping("/create")
    public Result<Boolean> createPermission(@Valid @RequestBody CreatePermissionDto dto) {
        return Result.success(permissionService.createPermission(dto));
    }

    @Operation(summary = "修改权限",
            description = "仅超级管理员可操作，检查内置权限及授权边界。")
    @PreAuthorize("@securityUtils.isSuperAdmin()")
    @PutMapping("/update")
    public Result<Boolean> updatePermission(@Valid @RequestBody UpdatePermissionDto dto) {
        return Result.success(permissionService.updatePermission(dto));
    }

    @Operation(summary = "删除权限",
            description = "仅超级管理员可操作，检查内置权限保护并清理角色关联。")
    @PreAuthorize("@securityUtils.isSuperAdmin()")
    @DeleteMapping("/remove/{permissionId}")
    public Result<Boolean> removePermission(@Parameter(description = "权限编号") @PathVariable Long permissionId) {
        return Result.success(permissionService.removePermission(permissionId));
    }
}
