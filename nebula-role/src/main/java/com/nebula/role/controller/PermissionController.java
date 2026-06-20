package com.nebula.role.controller;


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
public class PermissionController {

    private final PermissionService permissionService;

    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping
    public Result<Map<String, List<PermissionVO>>> getPermissions() {
        return Result.success(permissionService.getPermissions());
    }

    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping("/{roleId}")
    public Result<Map<String, List<PermissionVO>>> getRolePermissions(@PathVariable String roleId) {
        return Result.success(permissionService.getPermissionByRoleId(roleId));
    }

    @PreAuthorize("hasAuthority('user:create')")
    @PostMapping("/save")
    public Result<Boolean> saveRolePermissions(@RequestBody SaveRolePermissionsDto dto) {
        return Result.success(permissionService.assignPermissionsToRole(dto));
    }

    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping("/page")
    public Result<Page<PermissionVO>> getPermissionsPage(@Valid PermissionPageDto dto) {
        return Result.success(permissionService.getPermissionPage(dto));
    }

    @PreAuthorize("hasAuthority('user:create')")
    @PostMapping("/create")
    public Result<Boolean> createPermission(@Valid @RequestBody CreatePermissionDto dto) {
        return Result.success(permissionService.createPermission(dto));
    }

    @PreAuthorize("hasAuthority('user:update')")
    @PutMapping("/update")
    public Result<Boolean> updatePermission(@Valid @RequestBody UpdatePermissionDto dto) {
        return Result.success(permissionService.updatePermission(dto));
    }

    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/remove/{permissionId}")
    public Result<Boolean> removePermission(@PathVariable Long permissionId) {
        return Result.success(permissionService.removePermission(permissionId));
    }
}
