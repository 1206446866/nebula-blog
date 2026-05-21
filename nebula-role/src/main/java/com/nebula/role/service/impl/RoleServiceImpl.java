package com.nebula.role.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.role.entity.Role;
import com.nebula.role.mapper.RoleMapper;
import com.nebula.role.service.RoleService;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.nebula.role.entity.table.RoleTableDef.ROLE;
import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final UserRoleMapper userRoleMapper;

    @Override
    public Page<Role> pageRoles(int page, int size, String roleName) {
        QueryWrapper query = new QueryWrapper();
        if (roleName != null && !roleName.isEmpty()) {
            query.like("name", roleName);
        }
        query.orderBy(ROLE.CREATE_TIME.desc());
        Page<Role> pageObj = new Page<>(page, size);
        this.page(pageObj, query);
        return pageObj;
    }

    @Override
    public List<Role> getRolesByUserId(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectListByCondition(USER_ROLE.USER_ID.eq(userId));
        if (userRoles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        return  list(ROLE.ID.in(roleIds));
    }

    @Override
    public boolean assignRolesToUser(Long userId, List<Long> roleIds) {
        List<UserRole> toInsert = roleIds.stream().map(roleId -> new UserRole().setUserId(userId).setRoleId(roleId)).toList();
        int inserted = userRoleMapper.insertBatchSelective(toInsert);
        return inserted > 0;
    }

    @Override
    public boolean removeUserRole(Long userId, Long roleId) {
        QueryWrapper queryWrapper = QueryWrapper.create().where(USER_ROLE.USER_ID.eq(userId)).and(USER_ROLE.ROLE_ID.eq(roleId));
        return userRoleMapper.deleteByQuery(queryWrapper) > 0;
    }

}