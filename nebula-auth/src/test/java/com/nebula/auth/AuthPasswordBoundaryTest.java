package com.nebula.auth;

import com.nebula.auth.dto.ResetPasswordDTO;
import com.nebula.auth.service.impl.AuthServiceImpl;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.security.LoginUser;
import com.nebula.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static com.nebula.common.exception.code.AuthErrorCode.PASSWORD_POLICY_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/** 直接调用密码服务时也应拒绝空输入和缺少有效登录主体的请求，不连接数据库。 */
@ExtendWith(MockitoExtension.class)
class AuthPasswordBoundaryTest {
    @Mock UserMapper users;
    @InjectMocks AuthServiceImpl auth;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminAuthoritiesWithoutLoginUserCannotResetPassword() {
        authenticate("非用户主体");
        assertThrows(AccessDeniedException.class, () -> auth.resetPassword(2L, new ResetPasswordDTO()));
        verifyNoInteractions(users);
    }

    @Test
    void missingChangeRequestReturnsPasswordValidationError() {
        var operator = mock(LoginUser.class);
        when(operator.getPassword()).thenReturn("测试哈希");
        authenticate(operator);
        var error = assertThrows(BusinessException.class, () -> auth.changePassword(null));
        assertEquals(PASSWORD_POLICY_INVALID.getCode(), error.getCode());
        verifyNoInteractions(users);
    }

    @Test
    void missingResetRequestKeepsPasswordValidationError() {
        var operator = mock(LoginUser.class);
        when(operator.getUserId()).thenReturn(1L);
        authenticate(operator);
        var error = assertThrows(BusinessException.class, () -> auth.resetPassword(2L, null));
        assertEquals(PASSWORD_POLICY_INVALID.getCode(), error.getCode());
        verifyNoInteractions(users);
    }

    /** 同时提供管理员角色和管理权限，验证服务确实检查了登录主体本身。 */
    private void authenticate(Object principal) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("user:update"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }
}
