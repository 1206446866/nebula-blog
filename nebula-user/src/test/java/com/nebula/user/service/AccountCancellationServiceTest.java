package com.nebula.user.service;

import com.nebula.common.exception.BusinessException;
import com.nebula.common.security.LoginUser;
import com.nebula.common.security.RoleCatalog;
import com.nebula.user.dto.CancelAccountDTO;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import com.nebula.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.nebula.common.security.SensitivePasswordVerifier;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountCancellationServiceTest {
    private final UserMapper users = mock(UserMapper.class);
    private final UserRoleMapper roles = mock(UserRoleMapper.class);
    private final SensitivePasswordVerifier passwords = mock(SensitivePasswordVerifier.class);
    private final UserServiceImpl service = new UserServiceImpl(roles, passwords, mock(RoleCatalog.class), mock(com.nebula.media.service.MediaService.class));
    private User current;
    private CancelAccountDTO request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "mapper", users);
        LoginUser principal = mock(LoginUser.class);
        when(principal.getUserId()).thenReturn(42L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        current = User.create().setId(42L).setPassword("current-db-hash").setStatus(0).setDeleted(0);
        when(users.selectOneById(42L)).thenReturn(current);
        when(passwords.matches("current-password", "current-db-hash")).thenReturn(true);
        when(users.deleteByQuery(any())).thenReturn(1);
        request = new CancelAccountDTO();
        request.setPassword("current-password");
        request.setConfirmed(true);
    }

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test
    void deletesOnlyTheAuthenticatedAccountAfterVerifyingTheCurrentDatabasePassword() {
        assertTrue(service.cancelCurrentAccount(request));
        var ordered = inOrder(users, passwords, roles);
        ordered.verify(users).selectOneById(42L);
        ordered.verify(passwords).matches("current-password", "current-db-hash");
        ordered.verify(users).deleteByQuery(any());
        ordered.verify(roles).deleteByCondition(any());
    }

    @Test
    void wrongPasswordCannotDeleteTheAccountOrItsRoles() {
        request.setPassword("wrong");
        assertThrows(BusinessException.class, () -> service.cancelCurrentAccount(request));
        verify(users, never()).deleteByQuery(any());
        verifyNoInteractions(roles);
    }

    @Test
    void requiresExplicitConfirmationBeforeLoadingOrDeletingData() {
        request.setConfirmed(false);
        assertThrows(BusinessException.class, () -> service.cancelCurrentAccount(request));
        verifyNoInteractions(users, roles, passwords);
    }

    @Test
    void anonymousUsersCannotCancelAnAccount() {
        SecurityContextHolder.clearContext();
        assertThrows(BusinessException.class, () -> service.cancelCurrentAccount(request));
        verifyNoInteractions(users, roles, passwords);
    }

    @Test
    void missingOrDisabledUsersCannotCancelAnAccount() {
        when(users.selectOneById(42L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.cancelCurrentAccount(request));
        when(users.selectOneById(42L)).thenReturn(current.setStatus(1));
        assertThrows(BusinessException.class, () -> service.cancelCurrentAccount(request));
        verify(users, never()).deleteByQuery(any());
        verifyNoInteractions(roles, passwords);
    }

    @Test
    void concurrentChangesCannotProduceASuccessOrDeleteRoles() {
        when(users.deleteByQuery(any())).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.cancelCurrentAccount(request));
        verify(roles, never()).deleteByCondition(any());
    }
}
