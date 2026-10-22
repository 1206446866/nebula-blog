package com.nebula.api;

import com.nebula.auth.controller.AuthController;
import com.nebula.auth.dto.ChangePasswordDTO;
import com.nebula.auth.service.AuthService;
import com.nebula.common.mail.service.MailService;
import com.nebula.user.controller.UserController;
import com.nebula.user.dto.UpdateNameDTO;
import com.nebula.user.dto.CancelAccountDTO;
import com.nebula.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfileSelfServiceSecurityTest {
    private AnnotationConfigApplicationContext context;

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean UserService users() { return mock(UserService.class); }
        @Bean AuthService auth() { return mock(AuthService.class); }
        @Bean MailService mail() { return mock(MailService.class); }
        @Bean UserController userController(UserService users) { return new UserController(users); }
        @Bean com.nebula.auth.service.AuthAbuseProtection protection() { return mock(com.nebula.auth.service.AuthAbuseProtection.class); }
        @Bean AuthController authController(AuthService auth, com.nebula.auth.service.AuthAbuseProtection protection) { return new AuthController(auth, protection); }
    }

    @BeforeEach
    void setUp() { context = new AnnotationConfigApplicationContext(Config.class); }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); context.close(); }

    private void authenticateOrdinaryUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ordinary-user", null, List.of()));
    }

    @Test
    void ordinaryUsersCanRenameTheirOwnAccount() {
        authenticateOrdinaryUser();
        when(context.getBean(UserService.class).rename(any())).thenReturn(true);
        UpdateNameDTO dto = new UpdateNameDTO();
        dto.setUsername("Nebula");
        assertTrue(context.getBean(UserController.class).rename(dto).getData());
    }

    @Test
    void ordinaryUsersCanUseTheSelfPasswordEndpoint() {
        authenticateOrdinaryUser();
        when(context.getBean(AuthService.class).changePassword(any())).thenReturn(true);
        assertTrue(context.getBean(AuthController.class).changePassword(new ChangePasswordDTO()).getData());
    }

    @Test
    void anonymousUsersCannotUseSelfServiceEndpoints() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> context.getBean(UserController.class).rename(new UpdateNameDTO()));
        verifyNoInteractions(context.getBean(UserService.class));
    }

    @Test
    void administratorResetStillRequiresItsOriginalPermission() {
        authenticateOrdinaryUser();
        assertThrows(AccessDeniedException.class, () -> context.getBean(AuthController.class).resetPassword(2L, new com.nebula.auth.dto.ResetPasswordDTO()));
        verifyNoInteractions(context.getBean(AuthService.class));
    }

    @Test
    void ordinaryUsersCanUseTheSelfCancellationEndpoint() {
        authenticateOrdinaryUser();
        when(context.getBean(UserService.class).cancelCurrentAccount(any())).thenReturn(true);
        assertTrue(context.getBean(UserController.class).cancelCurrentAccount(new CancelAccountDTO()).getData());
    }

    @Test
    void anonymousUsersCannotCancelAnAccount() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> context.getBean(UserController.class).cancelCurrentAccount(new CancelAccountDTO()));
        verifyNoInteractions(context.getBean(UserService.class));
    }
}
