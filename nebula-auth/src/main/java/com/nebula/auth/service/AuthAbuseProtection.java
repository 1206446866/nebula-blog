package com.nebula.auth.service;

import com.nebula.auth.security.AuthAbuseProperties;
import com.nebula.auth.security.AuthProtectionUnavailableException;
import com.nebula.auth.security.AuthRateLimitException;
import com.nebula.common.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthAbuseProtection {
    private static final DefaultRedisScript<Long> ACQUIRE = new DefaultRedisScript<>("""
            local count = tonumber(redis.call('GET', KEYS[1]) or '0')
            if count >= tonumber(ARGV[1]) then
                local ttl = redis.call('PTTL', KEYS[1])
                if ttl < 1 then redis.call('PEXPIRE', KEYS[1], ARGV[2]); ttl = tonumber(ARGV[2]) end
                return math.ceil(ttl / 1000)
            end
            local next = redis.call('INCR', KEYS[1])
            if next == 1 or redis.call('PTTL', KEYS[1]) < 1 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end
            return 0
            """, Long.class);
    private final StringRedisTemplate redis;
    private final ClientIpResolver clientIps;
    private final AuthAbuseProperties properties;

    public int emailCooldownSeconds() { return properties.getEmailCooldownSeconds(); }

    public void requireLoginIp(HttpServletRequest request) {
        acquire("login-ip", clientIps.getIp(request), properties.getLoginIpRequests(), properties.getLoginIpWindowSeconds());
    }
    public void requireEmailIp(HttpServletRequest request) {
        acquire("email-ip", clientIps.getIp(request), properties.getEmailIpRequests(), properties.getEmailIpWindowSeconds());
    }
    public void requireVerifyIp(HttpServletRequest request) {
        acquire("verify-ip", clientIps.getIp(request), properties.getVerifyIpRequests(), properties.getVerifyIpWindowSeconds());
    }
    public void requireCredentialAttempt(String purpose, String subject) {
        acquire(purpose, normalize(subject), properties.getCredentialAttempts(), properties.getCredentialWindowSeconds());
    }
    public void credentialSucceeded(String purpose, String subject) {
        try {
            redis.delete(key(purpose, normalize(subject)));
        } catch (DataAccessException | SerializationException e) {
            throw new AuthProtectionUnavailableException(e);
        }
    }
    private void acquire(String purpose, String subject, int limit, int seconds) {
        try {
            Long wait = redis.execute(ACQUIRE, List.of(key(purpose, subject)), String.valueOf(limit), String.valueOf(seconds * 1000L));
            if (wait == null) throw new AuthProtectionUnavailableException();
            if (wait > 0) throw new AuthRateLimitException(wait);
        } catch (DataAccessException | SerializationException e) {
            throw new AuthProtectionUnavailableException(e);
        }
    }
    private String key(String purpose, String subject) {
        return properties.getKeyPrefix() + purpose + ":" + digest(subject);
    }
    private static String normalize(String subject) { return subject.strip().toLowerCase(Locale.ROOT); }
    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
