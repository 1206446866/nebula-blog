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
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TokenSessionServiceTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    @SuppressWarnings("unchecked")
    private final ZSetOperations<String, String> devices = mock(ZSetOperations.class);
    private final TokenSessionService sessions = new TokenSessionService(redis);
    private final String id = UUID.randomUUID().toString();
    private final User user = User.create().setId(42L).setPassword("stored-bcrypt-hash");

    @BeforeEach
    void setup() {
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForZSet()).thenReturn(devices);
    }

    @Test
    void sessionHasBoundedLifetimeAndBindsToBothUserAndCurrentPassword() {
        var now = Instant.now();
        when(redis.execute(any(RedisScript.class), anyList(), any(), any(), any(), any())).thenReturn(1L);
        sessions.register(id, user, now, now.plusSeconds(600), "test");
        var value = ArgumentCaptor.forClass(String.class);
        verify(redis).execute(any(RedisScript.class), eq(List.of("auth:session:" + id, "auth:session-devices:42:0")),
                any(), eq(Long.toString(now.plusSeconds(600).toEpochMilli())), eq(id), value.capture());
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
        when(redis.execute(any(RedisScript.class), anyList(), any(), any(), any(), any())).thenReturn(0L);
        assertThrows(TokenSessionUnavailableException.class,
                () -> sessions.register(id, user, Instant.now(), Instant.now().plusSeconds(600), null));
    }

    @Test
    void redisReadWriteAndDeleteFailuresAreNeverSilentlyAccepted() {
        var failure = new DataAccessResourceFailureException("test outage");
        when(values.get(anyString())).thenThrow(failure);
        when(redis.execute(any(RedisScript.class), anyList(), any(), any(), any(), any())).thenThrow(failure);
        when(redis.delete(anyString())).thenThrow(failure);
        assertThrows(TokenSessionUnavailableException.class, () -> sessions.isActive(id, user));
        assertThrows(TokenSessionUnavailableException.class,
                () -> sessions.register(id, user, Instant.now(), Instant.now().plusSeconds(600), null));
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

    @Test
    void deviceListSkipsRevokedSessionsAndMarksOnlyCurrentSession() {
        var other = UUID.randomUUID().toString();
        var now = Instant.now();
        when(redis.execute(any(RedisScript.class), anyList(), any(), any(), any(), any())).thenReturn(1L);
        sessions.register(id, user, now, now.plusSeconds(600), null);
        var captured = ArgumentCaptor.forClass(String.class);
        verify(redis).execute(any(RedisScript.class), anyList(), any(), any(), eq(id), captured.capture());
        when(devices.reverseRangeByScore(eq("auth:session-devices:42:0"), anyDouble(), anyDouble()))
                .thenReturn(new java.util.LinkedHashSet<>(List.of(id, other)));
        when(values.multiGet(List.of("auth:session:" + id, "auth:session:" + other)))
                .thenReturn(Arrays.asList(captured.getValue(), null));
        var result = sessions.listDevices(user, id);
        assertEquals(1, result.size());
        assertTrue(result.get(0).current());
        assertEquals("未知系统", result.get(0).os());
        user.setSessionVersion(3L);
        when(devices.reverseRangeByScore(eq("auth:session-devices:42:3"), anyDouble(), anyDouble())).thenReturn(Set.of());
        assertTrue(sessions.listDevices(user, id).isEmpty());
    }
}
