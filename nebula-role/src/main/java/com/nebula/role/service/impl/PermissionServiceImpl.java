package com.nebula.role.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.role.dto.SaveRolePermissionsDto;
import com.nebula.role.entity.Permission;
import com.nebula.role.entity.RolePermission;
import com.nebula.role.mapper.PermissionMapper;
import com.nebula.role.mapper.RolePermissionMapper;
import com.nebula.role.service.PermissionService;
import com.nebula.role.vo.PermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.nebula.role.entity.table.PermissionTableDef.PERMISSION;
import static com.nebula.role.entity.table.RolePermissionTableDef.ROLE_PERMISSION;
import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;


    @Override
    public Map<String, List<PermissionVO>> getPermissionByRoleId(String roleId) {
        List<RolePermission> rp = rolePermissionMapper.selectListByCondition(ROLE_PERMISSION.ROLE_ID.eq(roleId));
        List<Permission> list = permissionMapper.selectListByCondition(PERMISSION.ID.in(rp.stream().map(RolePermission::getPermissionId).toList()));
        return list.stream()
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveRolePermissions(SaveRolePermissionsDto dto) {
        rolePermissionMapper.deleteByCondition(ROLE_PERMISSION.ROLE_ID.eq(dto.getRoleId()));
        if (Objects.nonNull(dto.getPermissionIds()) && !dto.getPermissionIds().isEmpty()) {
            rolePermissionMapper.insertBatchSelective(dto.getPermissionIds().stream().map(item -> RolePermission.create().setRoleId(dto.getRoleId()).setPermissionId(item)).toList());
        }
        return true;
    }

    @Override
    public boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        List<RolePermission> toInsert = permissionIds.stream().map(pid -> RolePermission.create().setRoleId(roleId).setPermissionId(pid)).toList();

        int inserted = rolePermissionMapper.insertBatch(toInsert);
        return inserted > 0;
    }

    @Override
    public boolean removePermissionFromRole(Long roleId, Long permissionId) {
        var deleted = rolePermissionMapper.deleteByCondition(ROLE_PERMISSION.ROLE_ID.eq(roleId).and(ROLE_PERMISSION.PERMISSION_ID.eq(permissionId)));
        return deleted > 0;
    }

    @Override
    public List<String> getPermissionsByUserId(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(PERMISSION.NAME)
                .from(PERMISSION)
                .innerJoin(ROLE_PERMISSION)
                .on(PERMISSION.ID.eq(ROLE_PERMISSION.PERMISSION_ID))
                .innerJoin(USER_ROLE)
                .on(ROLE_PERMISSION.ROLE_ID.eq(USER_ROLE.ROLE_ID))
                .where(USER_ROLE.USER_ID.eq(userId));
        List<Permission> permissions = permissionMapper.selectListByQuery(queryWrapper);
        return permissions.stream().map(Permission::getName).distinct().toList();
    }

}