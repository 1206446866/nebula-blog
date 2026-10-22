package com.nebula.user.service;

import com.nebula.common.exception.BusinessException;
import com.nebula.common.security.LoginUser;
import com.nebula.common.security.RoleCatalog;
import com.nebula.user.dto.CancelAccountDTO;
import com.nebula.user.entity.User;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import com.nebula.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LastSuperAdminProtectionTest {
    private final UserMapper users = mock(UserMapper.class);
    private final UserRoleMapper roles = mock(UserRoleMapper.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final UserServiceImpl service = new UserServiceImpl(roles, passwords, mock(RoleCatalog.class));
    private User current;
    private CancelAccountDTO cancellation;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "mapper", users);
        LoginUser principal = mock(LoginUser.class);
        when(principal.getUserId()).thenReturn(42L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));
        current = User.create().setId(42L).setStatus(0).setDeleted(0).setPassword("current-hash");
        when(users.selectOneById(42L)).thenReturn(current);
        when(passwords.matches("current-password", "current-hash")).thenReturn(true);
        when(roles.selectCountByCondition(any())).thenReturn(1L);
        when(roles.selectListByQuery(any())).thenReturn(List.of(UserRole.create().setUserId(42L).setRoleId(1L)));
        when(users.selectListByQuery(any())).thenReturn(List.of(current));
        cancellation = new CancelAccountDTO();
        cancellation.setPassword("current-password");
        cancellation.setConfirmed(true);
    }

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test
    void cannotCancelTheLastActiveSuperAdministrator() {
        assertThrows(BusinessException.class, () -> service.cancelCurrentAccount(cancellation));
        verify(users, never()).deleteByQuery(any());
        verify(roles, never()).deleteByCondition(any());
    }

    @Test
    void cannotDisableOrDeleteTheLastActiveSuperAdministrator() {
        assertThrows(BusinessException.class, () -> service.switchStatusById(42L, 1));
        assertThrows(BusinessException.class, () -> service.deleteUserById(42L));
        verify(users, never()).update(any(User.class));
        verify(users, never()).deleteById(42L);
    }

    @Test
    void canCancelAfterAnotherActiveSuperAdministratorExists() {
        when(roles.selectListByQuery(any())).thenReturn(List.of(UserRole.create().setUserId(42L).setRoleId(1L),
                UserRole.create().setUserId(43L).setRoleId(1L)));
        when(users.selectListByQuery(any())).thenReturn(List.of(current, User.create().setId(43L).setStatus(0)));
        when(users.deleteByQuery(any())).thenReturn(1);
        assertTrue(service.cancelCurrentAccount(cancellation));
        verify(roles).deleteByCondition(any());
    }
}
