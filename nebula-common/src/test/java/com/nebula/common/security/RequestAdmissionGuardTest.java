package com.nebula.common.security;

import com.nebula.common.exception.ResourceLimitException;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

/** 不连接外部依赖，检查频率、并发、来源表容量及名额所有权。 */
class RequestAdmissionGuardTest {
    @Test void oneSourceCannotResetRateByChangingRoutesOrCompletingRequests() {
        var limits = new RequestProtectionProperties(); limits.setIpRequests(2);
        var now = new AtomicLong(); var guard = new RequestAdmissionGuard(limits, now::get);
        guard.enter("192.0.2.1").close(); guard.enter("192.0.2.1").close();
        var error = assertThrows(ResourceLimitException.class, () -> guard.enter("192.0.2.1"));
        assertEquals(429, error.getHttpStatus()); assertEquals(60, error.getRetryAfterSeconds());
        try (var other = guard.enter("192.0.2.2")) { assertNotNull(other); }
        now.set(60_000_000_000L);
        try (var recovered = guard.enter("192.0.2.1")) { assertNotNull(recovered); }
    }

    @Test void sourceAndGlobalConcurrencyReleaseExactlyOnce() {
        var limits = new RequestProtectionProperties(); limits.setIpConcurrency(1); limits.setMaxConcurrency(2);
        var guard = new RequestAdmissionGuard(limits);
        var first = guard.enter("192.0.2.1");
        assertEquals(429, assertThrows(ResourceLimitException.class, () -> guard.enter("192.0.2.1")).getHttpStatus());
        var second = guard.enter("192.0.2.2");
        assertThrows(ResourceLimitException.class, () -> guard.enter("192.0.2.3"));
        first.close(); first.close();
        try (var third = guard.enter("192.0.2.3")) {
            assertThrows(ResourceLimitException.class, () -> guard.enter("192.0.2.4"));
        }
        second.close();
        try (var recovered = guard.enter("192.0.2.1")) { assertNotNull(recovered); }
    }

    @Test void fullSourceTableCannotEvictLiveRateCounters() {
        var limits = new RequestProtectionProperties(); limits.setMaxSources(2); limits.setIpRequests(1);
        var now = new AtomicLong(); var guard = new RequestAdmissionGuard(limits, now::get);
        guard.enter("192.0.2.1").close(); guard.enter("192.0.2.2").close();
        for (int i=3; i<20; i++) {
            String source = "192.0.2."+i;
            assertEquals(503, assertThrows(ResourceLimitException.class, () -> guard.enter(source)).getHttpStatus());
        }
        assertEquals(429, assertThrows(ResourceLimitException.class, () -> guard.enter("192.0.2.1")).getHttpStatus());
        now.set(60_000_000_000L);
        try (var recovered = guard.enter("192.0.2.3")) { assertNotNull(recovered); }
    }

    @Test void expiredSourceWithActiveRequestCannotBeRemovedOrGrantExtraConcurrency() {
        var limits = new RequestProtectionProperties(); limits.setMaxSources(1); limits.setIpConcurrency(1);
        var now = new AtomicLong(); var guard = new RequestAdmissionGuard(limits, now::get);
        var held = guard.enter("192.0.2.1"); now.set(61_000_000_000L);
        assertEquals(503, assertThrows(ResourceLimitException.class, () -> guard.enter("192.0.2.2")).getHttpStatus());
        assertEquals(429, assertThrows(ResourceLimitException.class, () -> guard.enter("192.0.2.1")).getHttpStatus());
        held.close(); now.addAndGet(61_000_000_000L);
        try (var recovered = guard.enter("192.0.2.2")) { assertNotNull(recovered); }
    }
}
