package com.nebula.common;

import com.nebula.common.security.*;
import com.nebula.common.util.ClientIpResolver;
import com.nebula.common.exception.ResourceLimitException;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;

class SensitivePasswordVerifierTest {
    @AfterEach void clean(){SecurityContextHolder.clearContext();}
    @Test void missingRedisResultRejectsBeforeComputingPassword() {
        var redis = mock(StringRedisTemplate.class);
        var passwords = mock(PasswordEncoder.class);
        var user = mock(LoginUser.class);
        when(user.getUserId()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        var verifier = new SensitivePasswordVerifier(redis, passwords, new ClientIpResolver(List.of()), new SensitivePasswordProperties());
        assertEquals(503, assertThrows(ResourceLimitException.class, () -> verifier.matches("123", "测试哈希")).getHttpStatus());
        verifyNoInteractions(passwords);
    }
    @Test void unavailableProtectionRejectsBeforeComputingPassword() {
        var redis=mock(StringRedisTemplate.class);var passwords=mock(PasswordEncoder.class);
        var user=mock(LoginUser.class);when(user.getUserId()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class),anyList(),any(Object[].class)))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("fixture"));
        var verifier=new SensitivePasswordVerifier(redis,passwords,new ClientIpResolver(List.of()),new SensitivePasswordProperties());
        assertEquals(503,assertThrows(ResourceLimitException.class,()->verifier.matches("1","fixture")).getHttpStatus());
        verifyNoInteractions(passwords);
    }
}
