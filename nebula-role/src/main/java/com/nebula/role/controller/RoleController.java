package com.nebula.role.controller;

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

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;


    /**
     * 获取角色列表
     */
    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping
    public Result<List<RoleVO>> getAllRoles() {
        return Result.success(roleService.findAll());
    }

    /**
     * 获取角色分页
     */
    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping("/page")
    public Result<Page<RoleVO>> getPageRoles(RolePageDto dto) {
        return Result.success(roleService.pageRoles(dto));
    }

    /**
     * 查询用户的所有角色
     */
    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping("/{userId}")
    public Result<List<Role>> getRolesByUserId(@PathVariable Long userId) {
        List<Role> roles = roleService.getRolesByUserId(userId);
        return Result.success(roles);
    }

    @PreAuthorize("hasAuthority('user:create')")
    @PostMapping("/create")
    public Result<Boolean> createRole(@RequestBody CreateRoleDto  dto) {
        return  Result.success(roleService.createRole(dto));
    }

    @PreAuthorize("hasAuthority('user:update')")
    @PutMapping("/update")
    public Result<Boolean> updateRole(@RequestBody UpdateRoleDto dto) {
        return Result.success(Role.create().setId(dto.getId()).setName(dto.getName()).setDescription(dto.getDescription()).updateById());
    }

    /**
     * 移除角色
     */
    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/remove/{roleId}")
    public Result<Boolean> removeRole(@PathVariable Long roleId) {
        return Result.success(roleService.removeRole(roleId));
    }

    /**
     * 给用户分配角色
     */
//    @PostMapping("/assign/{userId}")
//    public Result<Boolean> assignRolesToUser(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
//        return Result.success(roleService.assignRoles(userId, roleIds));
//    }



}