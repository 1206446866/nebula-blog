package com.nebula.user.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.nebula.user.entity.table.UserTableDef.USER;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    @Override
    public List<User> getUsers(String role, String username, int page, int size) {
        QueryWrapper query = new QueryWrapper()
                .where(USER.ROLE.eq(role).when(role != null && !role.isEmpty()));

        if (username != null && !username.isEmpty()) {
            query.like(User::getUsername, username);
        }

        query.orderBy(USER.ID.asc())
                .limit(page * size, size);

        return list(query);
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
}