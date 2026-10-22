package com.nebula.auth;

import com.nebula.auth.controller.AuthController;
import com.nebula.auth.security.RegistrationExceptionHandler;
import com.nebula.auth.security.RegistrationUnavailableException;
import com.nebula.auth.service.AuthService;
import com.nebula.common.exception.GlobalExceptionHandler;
import com.nebula.common.mail.service.MailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RegistrationEndpointTest {
    private final AuthService auth = mock(AuthService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(auth, mock(com.nebula.auth.service.AuthAbuseProtection.class)))
            .setControllerAdvice(new RegistrationExceptionHandler(), new GlobalExceptionHandler()).build();

    @Test
    void registrationStoreFailureIsRetryableAndContainsNoInternalError() throws Exception {
        when(auth.register(any())).thenThrow(new RegistrationUnavailableException(new IllegalStateException("private detail")));
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"0123456789abcdef0123456789abcdef\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("注册服务暂时不可用，请稍后重试"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void verificationStoreFailureCannotReportThatAnEmailWasSent() throws Exception {
        when(auth.verifyEmailCaptcha(any())).thenThrow(new RegistrationUnavailableException());
        mvc.perform(post("/auth/email/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"fixture@example.invalid\",\"code\":\"123456\",\"password\":\"test-password\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "../another-key"})
    void invalidLinkFormatsNeverReachTheService(String token) throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(auth);
    }

    @Test
    void invalidEmailCannotReachVerification() throws Exception {
        mvc.perform(post("/auth/email/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid-email\",\"code\":\"123456\",\"password\":\"test-password\"}"))
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(auth);
    }
}
