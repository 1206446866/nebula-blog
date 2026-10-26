package com.nebula.common;

import com.nebula.common.security.*;
import com.nebula.common.exception.ResourceLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ResourceGuardTest {
    @Test void missingRedisResultRejectsRequestsWithoutOccupyingWorkSlots() {
        var redis = mock(StringRedisTemplate.class);
        var limits = new ResourceLimitProperties();
        limits.setMaxConcurrency(1);
        var guard = new ResourceGuard(redis, limits);
        for (var work : new ResourceGuard.Work[]{ResourceGuard.Work.UPLOAD, ResourceGuard.Work.SEARCH,
                ResourceGuard.Work.READ, ResourceGuard.Work.WRITE}) {
            assertEquals(503, assertThrows(ResourceLimitException.class,
                    () -> guard.enter(work, "192.0.2.1", 1L)).getHttpStatus());
        }
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L);
        try (var permit = guard.enter(ResourceGuard.Work.READ, "192.0.2.1", 1L)) {
            assertNotNull(permit);
        }
    }
    @Test void redisFailureNeverFallsBackToUnlimitedRequests() {
        var redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("fixture unavailable"));
        var guard = new ResourceGuard(redis, new ResourceLimitProperties());
        var error = assertThrows(ResourceLimitException.class, () -> guard.enter(ResourceGuard.Work.SEARCH, "127.0.0.1", 1L));
        assertEquals(503, error.getHttpStatus());
        assertTrue(error.getRetryAfterSeconds() > 0);
    }
    @Test void permitsAreReleasedOnlyOnceAndWorkTypesHaveSeparateBudgets() {
        var limits = new ResourceLimitProperties(); limits.setDecodeConcurrency(1);
        var guard = new ResourceGuard(mock(StringRedisTemplate.class), limits);
        var first = guard.acquire(ResourceGuard.Work.DECODE);
        assertEquals(429, assertThrows(ResourceLimitException.class, () -> guard.acquire(ResourceGuard.Work.DECODE)).getHttpStatus());
        try (var independent = guard.acquire(ResourceGuard.Work.SEARCH)) { assertNotNull(independent); }
        first.close(); first.close();
        try (var second = guard.acquire(ResourceGuard.Work.DECODE)) {
            assertThrows(ResourceLimitException.class, () -> guard.acquire(ResourceGuard.Work.DECODE));
        }
    }
    @Test void differentEntryPointsShareTheSameOverallWorkBudget() {
        var redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class), anyList(), any(Object[].class))).thenReturn(0L);
        var limits = new ResourceLimitProperties(); limits.setMaxConcurrency(2);
        var guard = new ResourceGuard(redis, limits);
        var first = guard.enter(ResourceGuard.Work.READ, "192.0.2.1", 1L);
        try (var second = guard.enter(ResourceGuard.Work.READ, "192.0.2.2", 2L)) {
            assertThrows(ResourceLimitException.class, () -> guard.enter(ResourceGuard.Work.SEARCH, "192.0.2.3", 3L));
            first.close();
            try (var recovered = guard.enter(ResourceGuard.Work.SEARCH, "192.0.2.3", 3L)) { assertNotNull(recovered); }
        } finally { first.close(); }
    }
}
