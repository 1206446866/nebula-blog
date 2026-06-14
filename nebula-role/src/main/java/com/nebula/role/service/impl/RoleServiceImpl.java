package com.nebula.role.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.role.entity.Role;
import com.nebula.role.mapper.RoleMapper;
import com.nebula.role.service.RoleService;
import com.nebula.user.entity.User;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.nebula.role.entity.table.RoleTableDef.ROLE;
import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;
import static com.nebula.user.entity.table.UserTableDef.USER;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final UserMapper userMapper;
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
        List<Long> roleIds = userRoleMapper.selectListByCondition(USER_ROLE.USER_ID.eq(userId))
                .stream().map(UserRole::getRoleId).toList();
        if (roleIds.isEmpty()) return List.of();
        return getMapper().selectListByIds(roleIds);
    }

    @Override
    public List<Role> getRolesByUserNid(String Nid) {
        User user = userMapper.selectOneByCondition(USER.NID.eq(Nid));
        List<Long> roleIds = userRoleMapper.selectListByCondition(USER_ROLE.USER_ID.eq(user.getId()))
                .stream().map(UserRole::getRoleId).toList();
        if (roleIds.isEmpty()) return List.of();
        return getMapper().selectListByIds(roleIds);
    }

    @Override
    public List<User> getUsersByRoleId(Long roleId) {
        List<Long> userIds = userRoleMapper
                .selectListByCondition(USER_ROLE.ROLE_ID.eq(roleId))
                .stream()
                .map(UserRole::getUserId)
                .toList();

        if (userIds.isEmpty()) {
            return List.of();
        }

        return userMapper.selectListByIds(userIds);
    }

    @Override
    public boolean assignRoles(Long userId, List<Long> roleIds) {
        // 先删除已有角色
        userRoleMapper.deleteByCondition(USER_ROLE.USER_ID.eq(userId));
        // 再批量插入新角色
        List<UserRole> list = roleIds.stream()
                .map(roleId -> new UserRole().setUserId(userId).setRoleId(roleId))
                .toList();
        return userRoleMapper.insertBatch(list) > 0;
    }

    @Override
    public boolean removeRole(Long userId, Long roleId) {
        return userRoleMapper.deleteByCondition(USER_ROLE.USER_ID.eq(userId)
                .and(USER_ROLE.ROLE_ID.eq(roleId))) > 0;
    }


    @Override
    public List<Role> getAllRoles() {
        return list();
    }
}