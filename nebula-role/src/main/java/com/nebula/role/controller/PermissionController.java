package com.nebula.role.controller;


import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.nebula.common.result.Result;
import com.nebula.role.dto.CreatePermissionDto;
import com.nebula.role.dto.PermissionPageDto;
import com.nebula.role.dto.SaveRolePermissionsDto;
import com.nebula.role.dto.UpdatePermissionDto;
import com.nebula.role.entity.Permission;
import com.nebula.role.service.PermissionService;
import com.nebula.role.vo.PermissionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public Result<Map<String, List<PermissionVO>>> getPermissions() {
        List<Permission> list = permissionService.list();
        Map<String, List<PermissionVO>> grouped = list.stream()
                .map(p -> BeanUtil.toBean(p, PermissionVO.class))
                .collect(Collectors.groupingBy(p -> {
                    String name = p.getName(); // 你当前字段
                    if (name == null || name.isBlank()) {
                        return "other";
                    }
                    int idx = name.indexOf(':');
                    if (idx == -1) {
                        return "other";
                    }
                    return name.substring(0, idx);
                }));
        return Result.success(grouped);
    }

    @GetMapping("/{roleId}")
    public Result<Map<String, List<PermissionVO>>> getRolePermissions(@PathVariable String roleId) {
        return Result.success(permissionService.getPermissionByRoleId(roleId));
    }

    @PostMapping("/save")
    public Result<Boolean> saveRolePermissions(@RequestBody SaveRolePermissionsDto dto) {
        return Result.success(permissionService.assignPermissionsToRole(dto));
    }

    @GetMapping("/page")
    public Result<Page<PermissionVO>> getPermissionsPage(@Valid PermissionPageDto dto) {
        return Result.success(permissionService.getPermissionPage(dto));
    }

    @PostMapping("/create")
    public Result<Boolean> createPermission(@Valid @RequestBody CreatePermissionDto dto) {
        return Result.success(permissionService.createPermission(dto));
    }

    @PutMapping("/update")
    public Result<Boolean> updatePermission(@Valid @RequestBody UpdatePermissionDto dto) {
        return Result.success(permissionService.updatePermission(dto));
    }

    @DeleteMapping("/remove/{permissionId}")
    public Result<Boolean> removePermission(@PathVariable Long permissionId) {
        return Result.success(permissionService.removePermission(permissionId));
    }
}
