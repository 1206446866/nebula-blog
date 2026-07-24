package com.nebula.auth.service.impl;

import com.nebula.auth.AuthUserBuilder;
import com.nebula.common.exception.code.AuthErrorCode;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.nebula.user.entity.table.UserTableDef.USER;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    private final AuthUserBuilder authUserBuilder;

    @Override
    public UserDetails loadUserByUsername(String nid) throws UsernameNotFoundException {
        User user = userMapper.selectOneByCondition(USER.NID.eq(nid));
        if (user == null) {
            throw new UsernameNotFoundException(AuthErrorCode.USER_NOT_FOUND.getMessage());
        }
        return authUserBuilder.createLoginUser(user);
    }

}
