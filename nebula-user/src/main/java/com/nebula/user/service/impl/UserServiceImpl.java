package com.nebula.user.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import com.nebula.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.nebula.user.entity.table.UserTableDef.USER;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserRoleMapper userRoleMapper;

    @Override
    public Page<User> pageUsers(String role, String username, int page, int size) {
        QueryWrapper query = new QueryWrapper()
                .where(USER.ROLE.eq(role).when(role != null && !role.isEmpty()));

        if (username != null && !username.isEmpty()) {
            query.like(User::getUsername, username);
        }

        query.orderBy(USER.ID.asc());
        return page(Page.of(page,size),query);
    }

    @Override
    public long countUsers(String role, String username) {
        QueryWrapper query = new QueryWrapper()
                .where(USER.ROLE.eq(role).when(role != null && !role.isEmpty()));

        if (username != null && !username.isEmpty()) {
            query.like("username", username);
        }

        return count(query);
    }

    @Override
    public Page<User> getUsersByRole(String role,long current,long size) {
        QueryWrapper query = new QueryWrapper();
        query.select(USER.ID,USER.USERNAME,USER.ROLE,USER.STATUS,USER.CREATE_TIME);
        if (role != null && !role.isEmpty()) {
            query.eq(User::getRole, role);
        }
        query.orderBy(USER.ID.asc());
        return page(Page.of(current,size),query);
    }

    @Override
    public Boolean switchStatusById(Long userId, Integer status) {
        return updateById(User.create().setId(userId).setStatus(status));
    }

    @Override
    public boolean addUser(User user) {
        return save(user);
    }

    @Override
    public boolean updateUser(User user) {
        return updateById(user);
    }

//TODO
    @Override
    public boolean changePassword(Long userId, String newPassword) {
        return false;
    }
    //TODO
    @Override
    public boolean resetPassword(Long userId) {
        return false;
    }

    @Override
    public Boolean deleteUserById(Long id) {
        return removeById(id);
    }
}