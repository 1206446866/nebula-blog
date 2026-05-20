package com.nebula.start.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Spring Security 用户认证实现
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    public CustomUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 使用 MyBatis-Flex 查询用户
        User user = userMapper.selectOneByQuery(QueryWrapper.create().eq("username", username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 返回 Spring Security User 对象
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}