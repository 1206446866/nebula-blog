package com.nebula.auth.provider;

import com.nebula.auth.security.AuthLoginUser;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.token.PhoneAuthenticationToken;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static com.nebula.user.entity.table.UserTableDef.USER;

@Component
@RequiredArgsConstructor
public class PhoneAuthenticationProvider implements AuthenticationProvider {


    private final UserMapper userMapper;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) {

        String phone = authentication.getName();

        String password = authentication.getCredentials().toString();

        User user = userMapper.selectOneByCondition(USER.PHONE.eq(phone));

        if (user == null) {
            throw new BadCredentialsException("手机号或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("手机号或密码错误");
        }

        AuthLoginUser loginUser = authService.createLoginUser(user);

        return new PhoneAuthenticationToken(loginUser, loginUser.getAuthorities());
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return PhoneAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
