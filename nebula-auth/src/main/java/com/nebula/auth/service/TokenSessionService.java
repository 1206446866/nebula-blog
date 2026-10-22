package com.nebula.auth.service;

import com.nebula.auth.security.TokenSessionUnavailableException;
import com.nebula.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/** Active sessions only: missing state must never restore access to a signed JWT. */
@Service
@RequiredArgsConstructor
public class TokenSessionService {
    private static final String PREFIX = "auth:session:";
    private final StringRedisTemplate redis;

    public void register(String sessionId, User user, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("Token有效期必须大于零");
        try {
            if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key(sessionId), binding(user), ttl))) {
                throw new TokenSessionUnavailableException();
            }
        } catch (DataAccessException e) {
            throw new TokenSessionUnavailableException(e);
        }
    }

    public boolean isActive(String sessionId, User currentUser) {
        if (!validId(sessionId) || currentUser.getPassword() == null) return false;
        try {
            return binding(currentUser).equals(redis.opsForValue().get(key(sessionId)));
        } catch (DataAccessException e) {
            throw new TokenSessionUnavailableException(e);
        }
    }

    public void revoke(String sessionId) {
        if (!validId(sessionId)) return;
        try {
            if (redis.delete(key(sessionId)) == null) throw new TokenSessionUnavailableException();
        } catch (DataAccessException e) {
            throw new TokenSessionUnavailableException(e);
        }
    }

    private String binding(User user) {
        // Store neither the bearer token nor the database password hash in Redis.
        // Every request compares against the current database hash. A committed
        // password change invalidates all prior sessions without a Redis/SQL dual write.
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(user.getPassword().getBytes(StandardCharsets.UTF_8));
            return user.getId() + ":" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean validId(String id) {
        if (id == null) return false;
        try { return UUID.fromString(id).toString().equals(id); }
        catch (IllegalArgumentException e) { return false; }
    }

    private String key(String sessionId) { return PREFIX + sessionId; }
}
