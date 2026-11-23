package com.nebula.auth.util;

import com.nebula.user.entity.User;
import com.nebula.auth.service.TokenSessionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类
 */
@Component
public class JwtUtil {

    private final TokenSessionService sessions;
    private final long expire;
    private final SecretKey key;
    private final JwtParser parser;

    public JwtUtil(TokenSessionService sessions, @Value("${jwt.secret:}") String base64Secret,
                   @Value("${jwt.expire:7200000}") long expire) {
        this.sessions = sessions;
        if (expire <= 0) throw new IllegalStateException("jwt.expire must be positive (milliseconds)");
        this.expire = expire;
        this.key = signingKey(base64Secret);
        this.parser = Jwts.parser().verifyWith(key).build();
    }

    private static SecretKey signingKey(String base64Secret) {
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException("JWT signing key is missing; configure NEBULA_JWT_SECRET");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Secret.strip());
        } catch (IllegalArgumentException ignored) {
            // Do not attach decoder exceptions or configuration values to startup logs.
            throw new IllegalStateException("NEBULA_JWT_SECRET must be a Base64-encoded random key");
        }
        try {
            if (decoded.length < 32) {
                throw new IllegalStateException("NEBULA_JWT_SECRET must decode to at least 32 random bytes");
            }
            return Keys.hmacShaKeyFor(decoded);
        } finally {
            Arrays.fill(decoded, (byte) 0);
        }
    }

    /**
     * 生成 Token
     *
     * @param user 用户
     * @return JWT Token
     */
    public String createToken(User user) {
        return createToken(user, null);
    }

    /** 签发账号当前版本的令牌，同时登记该会话的设备信息。 */
    public String createToken(User user, String userAgent) {
        Date now = new Date();

        Date expireDate = new Date(now.getTime() + expire);

        String sessionId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(sessionId)
                // 用户唯一标识
                .subject(String.valueOf(user.getId()))
                .claim("sv", user.currentSessionVersion())
                // 签发时间
                .issuedAt(now)
                // 过期时间
                .expiration(expireDate)
                // 签名
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        sessions.register(sessionId, user, now.toInstant(), expireDate.toInstant(), userAgent);
        return token;
    }

    public boolean isSessionActive(Claims claims, User user) {
        return claims.getExpiration() != null && claims.getExpiration().after(new Date())
                && matchesSessionVersion(claims, user)
                && sessions.isActive(claims.getId(), user);
    }

    /** 升级前签发的令牌视为版本零，账号版本改变后立即失效。 */
    private boolean matchesSessionVersion(Claims claims, User user) {
        Object version = claims.get("sv");
        if (version == null) return user.currentSessionVersion() == 0;
        return (version instanceof Integer || version instanceof Long)
                && ((Number) version).longValue() == user.currentSessionVersion();
    }

    public void revokeToken(String token) {
        sessions.revoke(parseToken(token).getId());
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        var signed = parser.parseSignedClaims(token);
        if (!"HS256".equals(signed.getHeader().getAlgorithm())) {
            throw new UnsupportedJwtException("JWT signing algorithm must be HS256");
        }
        return signed.getPayload();
    }

    /**
     * 校验 Token
     */
    public boolean validateToken(String token) {

        try {

            parseToken(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    /**
     * 获取用户ID
     */
    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }
}
