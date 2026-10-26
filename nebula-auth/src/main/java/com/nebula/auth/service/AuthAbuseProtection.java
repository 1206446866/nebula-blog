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

    /** 读取邮件发送冷却秒数，供前端发送按钮倒计时使用。 */
    public int emailCooldownSeconds() { return properties.getEmailCooldownSeconds(); }

    /** 使用可信来源IP检查登录请求频率，超限或保护服务不可用时拒绝请求。 */
    public void requireLoginIp(HttpServletRequest request) {
        acquire("login-ip", clientIps.getIp(request), properties.getLoginIpRequests(), properties.getLoginIpWindowSeconds());
    }
    /** 按可信来源IP检查验证码邮件发送频率，防止轮换邮箱绕过限制。 */
    public void requireEmailIp(HttpServletRequest request) {
        acquire("email-ip", clientIps.getIp(request), properties.getEmailIpRequests(), properties.getEmailIpWindowSeconds());
    }
    /** 按可信来源IP检查邮箱验证频率，防止暴力尝试验证码。 */
    public void requireVerifyIp(HttpServletRequest request) {
        acquire("verify-ip", clientIps.getIp(request), properties.getVerifyIpRequests(), properties.getVerifyIpWindowSeconds());
    }
    /** 按认证用途和统一账号标识消耗尝试预算，让同一账号的登录别名共用限制。 */
    public void requireCredentialAttempt(String purpose, String subject) {
        acquire(purpose, normalize(subject), properties.getCredentialAttempts(), properties.getCredentialWindowSeconds());
    }
    /** 认证成功后清除对应尝试计数，Redis故障时不跳过安全保护。 */
    public void credentialSucceeded(String purpose, String subject) {
        try {
            redis.delete(key(purpose, normalize(subject)));
        } catch (DataAccessException | SerializationException e) {
            throw new AuthProtectionUnavailableException(e);
        }
    }
    /** 用Redis原子操作领取限额，超限返回剩余等待时间，故障时拒绝放行。 */
    private void acquire(String purpose, String subject, int limit, int seconds) {
        try {
            Long wait = redis.execute(ACQUIRE, List.of(key(purpose, subject)), String.valueOf(limit), String.valueOf(seconds * 1000L));
            if (wait == null) throw new AuthProtectionUnavailableException();
            if (wait > 0) throw new AuthRateLimitException(wait);
        } catch (DataAccessException | SerializationException e) {
            throw new AuthProtectionUnavailableException(e);
        }
    }
    /** 按用途和账号摘要生成隔离的限流键，不把明文账号放入键名。 */
    private String key(String purpose, String subject) {
        return properties.getKeyPrefix() + purpose + ":" + digest(subject);
    }
    /** 去除首尾空白并转为小写，使同一账号标识使用相同限流预算。 */
    private static String normalize(String subject) { return subject.strip().toLowerCase(Locale.ROOT); }
    /** 生成SHA-256摘要，避免Redis限流键暴露明文账号或来源标识。 */
    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
