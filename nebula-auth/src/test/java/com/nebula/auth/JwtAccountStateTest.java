package com.nebula.auth;

import com.nebula.auth.filter.JwtAuthenticationFilter;
import com.nebula.auth.util.JwtUtil;
import com.nebula.auth.security.TokenSessionUnavailableException;
import io.jsonwebtoken.Claims;
import com.nebula.role.service.PermissionService;
import com.nebula.role.service.RoleService;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAccountStateTest {
    private final JwtUtil jwt = mock(JwtUtil.class);
    private final UserMapper users = mock(UserMapper.class);
    private final RoleService roles = mock(RoleService.class);
    private final PermissionService permissions = mock(PermissionService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, users, roles, permissions);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("42");
        when(jwt.parseToken("existing-token")).thenReturn(claims);
        when(jwt.isSessionActive(any(), any())).thenReturn(true);
        when(roles.getRolesByUserId(42L)).thenReturn(List.of());
        when(permissions.getPermissionsByUserId(42L)).thenReturn(List.of());
    }

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    private void requestWithExistingToken() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/profile/42");
        request.addHeader("Authorization", "Bearer existing-token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    void normalAccountsRemainAuthenticated() throws Exception {
        when(users.selectOneByCondition(any())).thenReturn(User.create().setId(42L).setStatus(0).setDeleted(0));
        requestWithExistingToken();
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void removedAccountsCannotReuseExistingTokens() throws Exception {
        when(users.selectOneByCondition(any())).thenReturn(null);
        requestWithExistingToken();
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(roles, permissions);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ROLE_ADMIN"})
    void rolePrefixesAreAppliedOnlyOnceWhenReloadingJwtIdentity(String role) throws Exception {
        when(users.selectOneByCondition(any())).thenReturn(User.create().setId(42L).setStatus(0).setDeleted(0));
        when(roles.getRolesByUserId(42L)).thenReturn(List.of(com.nebula.role.entity.Role.create().setName(role)));
        requestWithExistingToken();
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority).toList();
        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertFalse(authorities.contains("ROLE_ROLE_ADMIN"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void disabledAndLockedAccountsCannotReuseExistingTokens(int status) throws Exception {
        when(users.selectOneByCondition(any())).thenReturn(User.create().setId(42L).setStatus(status).setDeleted(0));
        requestWithExistingToken();
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(roles, permissions);
    }

    @Test
    void deletedAccountsAreRejectedEvenIfAMapperReturnsThem() throws Exception {
        when(users.selectOneByCondition(any())).thenReturn(User.create().setId(42L).setStatus(0).setDeleted(1));
        requestWithExistingToken();
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(roles, permissions);
    }

    @Test
    void revokedSessionsCannotAuthenticate() throws Exception {
        when(users.selectOneByCondition(any())).thenReturn(User.create().setId(42L).setStatus(0).setDeleted(0));
        when(jwt.isSessionActive(any(), any())).thenReturn(false);
        requestWithExistingToken();
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(roles, permissions);
    }

    @Test
    void sessionStoreOutageFailsClosedWithRetryableStatus() throws Exception {
        when(users.selectOneByCondition(any())).thenReturn(User.create().setId(42L).setStatus(0).setDeleted(0));
        when(jwt.isSessionActive(any(), any())).thenThrow(new TokenSessionUnavailableException());
        var request = new MockHttpServletRequest("GET", "/users/42");
        request.addHeader("Authorization", "Bearer existing-token");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertEquals(503, response.getStatus());
        assertTrue(response.getContentAsString().contains("503"));
        assertNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(roles, permissions);
    }

    @Test
    void malformedSubjectsDoNotBecomeServerErrors() throws Exception {
        when(jwt.parseToken("existing-token").getSubject()).thenReturn("not-a-user-id");
        requestWithExistingToken();
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(users, roles, permissions);
    }
}
