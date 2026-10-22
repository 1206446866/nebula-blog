package com.nebula.auth;

import com.nebula.auth.controller.AuthController;
import com.nebula.auth.security.*;
import com.nebula.auth.service.AuthAbuseProtection;
import com.nebula.auth.service.AuthService;
import com.nebula.common.exception.GlobalExceptionHandler;
import com.nebula.common.util.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthAbuseProtectionTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final AuthAbuseProtection guard = new AuthAbuseProtection(redis, new ClientIpResolver(List.of()), new AuthAbuseProperties());

    @Test
    void limiterOutagesAndUnknownResultsFailClosed() {
        assertThrows(AuthProtectionUnavailableException.class, () -> guard.requireCredentialAttempt("login", "fixture"));
        doThrow(new DataAccessResourceFailureException("private outage detail")).when(redis)
                .execute(any(RedisScript.class), anyList(), any(), any());
        assertThrows(AuthProtectionUnavailableException.class, () -> guard.requireLoginIp(new MockHttpServletRequest()));
    }

    @Test
    void retryAfterComesFromTheAtomicCounter() {
        doReturn(42L).when(redis).execute(any(RedisScript.class), anyList(), any(), any());
        var limited = assertThrows(AuthRateLimitException.class, () -> guard.requireCredentialAttempt("login", "fixture"));
        assertEquals(42, limited.getRetryAfterSeconds());
    }

    @Test
    void unavailableProtectionNeverReachesAuthenticationAndHasASafeHttpResponse() throws Exception {
        var protection = mock(AuthAbuseProtection.class);
        var auth = mock(AuthService.class);
        doThrow(new AuthProtectionUnavailableException(new IllegalStateException("private outage detail")))
                .when(protection).requireLoginIp(any());
        var mvc = MockMvcBuilders.standaloneSetup(new AuthController(auth, protection))
                .setControllerAdvice(new AuthProtectionExceptionHandler(), new GlobalExceptionHandler()).build();
        mvc.perform(post("/auth/login").contentType("application/json").content("{\"account\":\"fixture\",\"password\":\"unused\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("认证安全服务暂时不可用，请稍后重试"));
        verifyNoInteractions(auth);
    }
}
