package com.nebula.auth;

import com.nebula.auth.security.AuthLoginUser;
import com.nebula.common.util.SecurityUtils;
import com.nebula.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuperAdminAuthoritiesTest {
    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @ParameterizedTest
    @ValueSource(strings = {"SUPER_ADMIN", "ROLE_SUPER_ADMIN"})
    void rootRoleSurvivesAnEmptyPermissionListAndInheritsAdmin(String role) {
        var principal = new AuthLoginUser(User.create().setId(42L), List.of(role), List.of());
        var names = principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertTrue(names.containsAll(List.of("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_USER")));
        assertFalse(names.stream().anyMatch(name -> name.startsWith("ROLE_ROLE_")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        assertTrue(SecurityUtils.isSuperAdmin());
        assertTrue(SecurityUtils.hasPermission("any:futurePermission"));
    }

    @Test
    void independentPermissionEntriesAreNotDiscardedWithoutRoles() {
        var principal = new AuthLoginUser(User.create().setId(42L), List.of(), List.of("article:list"));
        assertEquals(List.of("article:list"), principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }
}
