package com.nebula.role.controller;


import cn.hutool.core.bean.BeanUtil;
import com.nebula.common.result.Result;
import com.nebula.role.dto.SaveRolePermissionsDto;
import com.nebula.role.entity.Permission;
import com.nebula.role.service.PermissionService;
import com.nebula.role.vo.PermissionVO;
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
        return Result.success(permissionService.saveRolePermissions(dto));
    }
}
