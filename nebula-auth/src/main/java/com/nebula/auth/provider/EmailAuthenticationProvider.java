package com.nebula.auth.provider;

import com.nebula.auth.AuthUserBuilder;
import com.nebula.auth.security.AuthLoginUser;
import com.nebula.auth.token.EmailAuthenticationToken;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.nebula.user.entity.table.UserTableDef.USER;

@Component
@RequiredArgsConstructor
public class EmailAuthenticationProvider implements AuthenticationProvider {


    private final UserMapper userMapper;
    private final AuthUserBuilder authUserBuilder;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) {

        String email = authentication.getName();

        String password = authentication.getCredentials().toString();

        User user = userMapper.selectOneByCondition(USER.EMAIL.eq(email));

        if (Objects.isNull(user)) {
            throw new BadCredentialsException("邮箱或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("邮箱或密码错误");
        }

        AuthLoginUser loginUser = authUserBuilder.createLoginUser(user);

        return new EmailAuthenticationToken(loginUser, loginUser.getAuthorities());
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return EmailAuthenticationToken.class.isAssignableFrom(authentication);
    }
}