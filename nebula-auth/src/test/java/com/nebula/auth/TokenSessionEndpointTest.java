package com.nebula.auth;

import com.nebula.auth.controller.AuthController;
import com.nebula.auth.security.TokenSessionExceptionHandler;
import com.nebula.auth.security.TokenSessionUnavailableException;
import com.nebula.auth.service.AuthService;
import com.nebula.common.exception.GlobalExceptionHandler;
import com.nebula.common.mail.service.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TokenSessionEndpointTest {
    private final AuthService auth = mock(AuthService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(auth, mock(com.nebula.auth.service.AuthAbuseProtection.class)))
            .setControllerAdvice(new TokenSessionExceptionHandler(), new GlobalExceptionHandler()).build();

    @Test
    void loginMustNotReturnATokenWhenSessionRegistrationFails() throws Exception {
        when(auth.login(any())).thenThrow(new TokenSessionUnavailableException());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"fixture\",\"password\":\"test-password\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void logoutMustNotClaimSuccessWhenRevocationFails() throws Exception {
        doThrow(new TokenSessionUnavailableException()).when(auth).logout("fixture-token");
        mvc.perform(post("/auth/logout").header("Authorization", "Bearer fixture-token"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
