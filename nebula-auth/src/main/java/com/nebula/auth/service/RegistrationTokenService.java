package com.nebula.auth.service;

import com.nebula.auth.dto.RegisterCacheDTO;
import com.nebula.auth.security.RegistrationUnavailableException;
import com.nebula.auth.security.AuthAbuseProperties;
import com.nebula.auth.security.AuthRateLimitException;
import com.nebula.auth.util.RegistrationEmail;
import com.nebula.common.constant.RegisterType;
import com.nebula.common.constant.redisKey.AuthRedisKey;
import com.nebula.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.nebula.common.exception.code.RegistrationErrorCode.CAPTCHA_INVALID;
import static com.nebula.common.exception.code.RegistrationErrorCode.REGISTRATION_LINK_INVALID;
import static com.nebula.common.exception.code.RegistrationErrorCode.CAPTCHA_ATTEMPTS_EXCEEDED;

/** Security credentials use strict Redis operations, never the best-effort cache helper. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationTokenService {
    private static final long LINK_TTL_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final DefaultRedisScript<Long> VERIFY_AND_ISSUE = new DefaultRedisScript<>("""
            local stored = redis.call('GET', KEYS[1])
            if not stored then return 0 end
            if stored ~= ARGV[1] then
                local attempts = redis.call('INCR', KEYS[3])
                if attempts == 1 then redis.call('PEXPIRE', KEYS[3], math.max(1, redis.call('PTTL', KEYS[1]))) end
                if attempts >= tonumber(ARGV[4]) then redis.call('DEL', KEYS[1], KEYS[4]); return -2 end
                return 0
            end
            if redis.call('EXISTS', KEYS[2]) == 1 then return -1 end
            redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[3])
            redis.call('DEL', KEYS[1], KEYS[3], KEYS[4])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> CHECK_CAPTCHA = new DefaultRedisScript<>("""
            local stored = redis.call('GET', KEYS[1])
            if not stored then return 0 end
            if stored == ARGV[1] then return 1 end
            local attempts = redis.call('INCR', KEYS[2])
            if attempts == 1 then redis.call('PEXPIRE', KEYS[2], math.max(1, redis.call('PTTL', KEYS[1]))) end
            if attempts >= tonumber(ARGV[2]) then redis.call('DEL', KEYS[1], KEYS[3]); return -2 end
            return 0
            """, Long.class);
    private static final DefaultRedisScript<Long> ISSUE_CAPTCHA = new DefaultRedisScript<>("""
            local cooldown = redis.call('PTTL', KEYS[2])
            if cooldown > 0 then return math.ceil(cooldown / 1000) end
            local count = tonumber(redis.call('GET', KEYS[4]) or '0')
            if count >= tonumber(ARGV[5]) then
                local ttl = redis.call('PTTL', KEYS[4])
                if ttl < 1 then redis.call('PEXPIRE', KEYS[4], ARGV[6]); ttl = tonumber(ARGV[6]) end
                return math.ceil(ttl / 1000)
            end
            local next = redis.call('INCR', KEYS[4])
            if next == 1 then redis.call('PEXPIRE', KEYS[4], ARGV[6]) end
            redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[4])
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[3])
            redis.call('SET', KEYS[5], ARGV[2], 'PX', ARGV[3])
            redis.call('DEL', KEYS[3])
            return 0
            """, Long.class);
    private static final DefaultRedisScript<Long> DISCARD_CAPTCHA = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[2]) == ARGV[1] then redis.call('DEL', KEYS[1], KEYS[2], KEYS[3]); return 1 end
            return 0
            """, Long.class);
    private final RedisTemplate<String, Object> redis;
    private final AuthAbuseProperties properties;

    public String issueCaptcha(String email, String code) {
        String owner = UUID.randomUUID().toString();
        try {
            Long wait = redis.execute(ISSUE_CAPTCHA, List.of(AuthRedisKey.EMAIL_CAPTCHA + email,
                            AuthRedisKey.EMAIL_CAPTCHA_CD + email, AuthRedisKey.EMAIL_CAPTCHA_ATTEMPTS + email,
                            AuthRedisKey.EMAIL_SEND_WINDOW + email, AuthRedisKey.EMAIL_CAPTCHA_OWNER + email),
                    code, owner, 300_000L, properties.getEmailCooldownSeconds() * 1000L,
                    properties.getEmailMailboxRequests(), properties.getEmailMailboxWindowSeconds() * 1000L);
            if (wait == null) throw new RegistrationUnavailableException();
            if (wait > 0) throw new AuthRateLimitException(wait);
            return owner;
        } catch (DataAccessException | SerializationException e) {
            throw new RegistrationUnavailableException(e);
        }
    }

    public void discardCaptcha(String email, String owner) {
        try {
            redis.execute(DISCARD_CAPTCHA, List.of(AuthRedisKey.EMAIL_CAPTCHA + email,
                    AuthRedisKey.EMAIL_CAPTCHA_OWNER + email, AuthRedisKey.EMAIL_CAPTCHA_ATTEMPTS + email), owner);
        } catch (DataAccessException | SerializationException e) {
            log.warn("邮件发送失败后的验证码清理未完成，原有冷却和限额继续生效");
        }
    }

    /** Cheap rejection before BCrypt; the Lua operation below is the final atomic check. */
    public void requireCaptcha(String email, String code) {
        try {
            Long result = redis.execute(CHECK_CAPTCHA, List.of(AuthRedisKey.EMAIL_CAPTCHA + email,
                    AuthRedisKey.EMAIL_CAPTCHA_ATTEMPTS + email, AuthRedisKey.EMAIL_CAPTCHA_OWNER + email), code, properties.getCaptchaMaxFailures());
            checkCaptchaResult(result);
        } catch (DataAccessException | SerializationException e) {
            throw new RegistrationUnavailableException(e);
        }
    }

    public String verifyAndIssue(String email, String code, String encodedPassword) {
        String token = UUID.randomUUID().toString().replace("-", "");
        RegisterCacheDTO cache = new RegisterCacheDTO();
        cache.setAccount(email);
        cache.setPassword(encodedPassword);
        cache.setType(RegisterType.EMAIL);
        try {
            Long result = redis.execute(VERIFY_AND_ISSUE, List.of(
                    AuthRedisKey.EMAIL_CAPTCHA + email, AuthRedisKey.REGISTER_TOKEN + token,
                    AuthRedisKey.EMAIL_CAPTCHA_ATTEMPTS + email, AuthRedisKey.EMAIL_CAPTCHA_OWNER + email),
                    code, cache, LINK_TTL_MILLIS, properties.getCaptchaMaxFailures());
            checkCaptchaResult(result);
            return token;
        } catch (DataAccessException | SerializationException e) {
            throw new RegistrationUnavailableException(e);
        }
    }

    private void checkCaptchaResult(Long result) {
        if (Long.valueOf(-2).equals(result)) throw new BusinessException(CAPTCHA_ATTEMPTS_EXCEEDED);
        if (Long.valueOf(0).equals(result)) throw new BusinessException(CAPTCHA_INVALID);
        if (!Long.valueOf(1).equals(result)) throw new RegistrationUnavailableException();
    }

    public RegisterCacheDTO consume(String token) {
        if (token == null || !token.matches("[a-f0-9]{32}")) throw new BusinessException(REGISTRATION_LINK_INVALID);
        String key = AuthRedisKey.REGISTER_TOKEN + token;
        Instant startedAt = Instant.now();
        RegisterCacheDTO cache;
        long ttl;
        try {
            Long remaining = redis.getExpire(key, TimeUnit.MILLISECONDS);
            if (remaining == null) throw new RegistrationUnavailableException();
            if (remaining <= 0) throw new BusinessException(REGISTRATION_LINK_INVALID);
            ttl = remaining;
            // Redis GETDEL has one winner even across application instances.
            Object value = redis.opsForValue().getAndDelete(key);
            if (!(value instanceof RegisterCacheDTO registration)) throw new BusinessException(REGISTRATION_LINK_INVALID);
            cache = registration;
        } catch (DataAccessException | SerializationException e) {
            throw new RegistrationUnavailableException(e);
        }
        // Legacy links issued before this fix omitted type; they still represent
        // email registration. Explicit phone grants must never become email grants.
        if ((cache.getType() != null && cache.getType() != RegisterType.EMAIL)
                || cache.getAccount() == null || !cache.getAccount().contains("@")
                || cache.getAccount().length() > 128 || cache.getPassword() == null || cache.getPassword().isBlank()) {
            throw new BusinessException(REGISTRATION_LINK_INVALID);
        }
        cache.setAccount(RegistrationEmail.normalize(cache.getAccount()));
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            Instant expiresAt = startedAt.plusMillis(ttl);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    // Restore only after a confirmed SQL rollback, with the original
                    // deadline. Never restore after a commit or an unknown outcome.
                    if (status == STATUS_ROLLED_BACK) restore(key, cache, expiresAt);
                }
            });
        }
        return cache;
    }

    private void restore(String key, RegisterCacheDTO cache, Instant expiresAt) {
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        if (remaining.isNegative() || remaining.isZero()) return;
        try {
            redis.opsForValue().setIfAbsent(key, cache, remaining);
        } catch (DataAccessException | SerializationException e) {
            // Do not expose or log the credential key, email, or password hash.
            log.warn("注册回滚后的凭证恢复失败，用户需重新验证邮箱");
        }
    }
}
