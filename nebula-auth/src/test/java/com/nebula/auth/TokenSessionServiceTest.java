package com.nebula.auth;

import com.nebula.auth.security.TokenSessionUnavailableException;
import com.nebula.auth.service.TokenSessionService;
import com.nebula.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TokenSessionServiceTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final TokenSessionService sessions = new TokenSessionService(redis);
    private final String id = UUID.randomUUID().toString();
    private final User user = User.create().setId(42L).setPassword("stored-bcrypt-hash");

    @BeforeEach
    void setup() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    @Test
    void sessionHasBoundedLifetimeAndBindsToBothUserAndCurrentPassword() {
        sessions.register(id, user, Instant.now().plusSeconds(600));
        var value = ArgumentCaptor.forClass(String.class);
        var ttl = ArgumentCaptor.forClass(Duration.class);
        verify(values).setIfAbsent(eq("auth:session:" + id), value.capture(), ttl.capture());
        assertTrue(ttl.getValue().toMillis() > 590_000 && ttl.getValue().toMillis() <= 600_000);
        assertFalse(value.getValue().contains(user.getPassword()));
        when(values.get("auth:session:" + id)).thenReturn(value.getValue());
        assertTrue(sessions.isActive(id, user));
        assertFalse(sessions.isActive(id, User.create().setId(43L).setPassword(user.getPassword())));
        assertFalse(sessions.isActive(id, User.create().setId(42L).setPassword("new-bcrypt-hash")));
    }

    @Test
    void missingSessionDoesNotFallBackToSignatureOnlyAuthentication() {
        assertFalse(sessions.isActive(id, user));
    }

    @Test
    void legacyOrMalformedIdsNeverAccessRedis() {
        assertFalse(sessions.isActive(null, user));
        assertFalse(sessions.isActive("../../another-key", user));
        sessions.revoke(null);
        verifyNoInteractions(redis);
    }

    @Test
    void cannotIssueAnUnregisteredSession() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        assertThrows(TokenSessionUnavailableException.class,
                () -> sessions.register(id, user, Instant.now().plusSeconds(600)));
    }

    @Test
    void redisReadWriteAndDeleteFailuresAreNeverSilentlyAccepted() {
        var failure = new DataAccessResourceFailureException("test outage");
        when(values.get(anyString())).thenThrow(failure);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenThrow(failure);
        when(redis.delete(anyString())).thenThrow(failure);
        assertThrows(TokenSessionUnavailableException.class, () -> sessions.isActive(id, user));
        assertThrows(TokenSessionUnavailableException.class,
                () -> sessions.register(id, user, Instant.now().plusSeconds(600)));
        assertThrows(TokenSessionUnavailableException.class, () -> sessions.revoke(id));
    }

    @Test
    void unknownRevocationResultNeverReportsSuccess() {
        when(redis.delete("auth:session:" + id)).thenReturn(null);
        assertThrows(TokenSessionUnavailableException.class, () -> sessions.revoke(id));
    }

    @Test
    void repeatedRevocationIsSafe() {
        when(redis.delete("auth:session:" + id)).thenReturn(true, false);
        assertDoesNotThrow(() -> sessions.revoke(id));
        assertDoesNotThrow(() -> sessions.revoke(id));
        verify(redis, times(2)).delete("auth:session:" + id);
    }
}
