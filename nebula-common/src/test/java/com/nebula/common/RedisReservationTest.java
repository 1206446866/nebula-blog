package com.nebula.common;

import com.nebula.common.constant.UserStatus;
import com.nebula.common.redis.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 覆盖计数设施故障与状态反向判断，避免异常时误放行或重复计数。 */
class RedisReservationTest {
    @Test void reverseStatusHandlesEqualDifferentAndNull() {
        for (UserStatus status : UserStatus.values()) {
            assertFalse(status.notMatches(status.getCode()));
            assertTrue(status.notMatches(null));
            for (UserStatus other : UserStatus.values()) assertEquals(!status.matches(other.getCode()), status.notMatches(other.getCode()));
        }
    }

    @Test @SuppressWarnings("unchecked") void unavailableReservationDoesNotPretendToOwnTheBudget() {
        RedisTemplate<String,Object> template = mock(RedisTemplate.class);
        ValueOperations<String,Object> values = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        when(values.setIfAbsent("fixture", "owner", 60, TimeUnit.SECONDS)).thenThrow(new RedisConnectionFailureException("fixture"));
        RedisService service = new RedisService(); ReflectionTestUtils.setField(service,"redisTemplate",template);
        assertFalse(service.setIfAbsent("fixture","owner",60,TimeUnit.SECONDS));
        doReturn(null,false,true).when(values).setIfAbsent("fixture", "owner", 60, TimeUnit.SECONDS);
        assertFalse(service.setIfAbsent("fixture","owner",60,TimeUnit.SECONDS));
        assertFalse(service.setIfAbsent("fixture","owner",60,TimeUnit.SECONDS));
        assertTrue(service.setIfAbsent("fixture","owner",60,TimeUnit.SECONDS));
    }
}
