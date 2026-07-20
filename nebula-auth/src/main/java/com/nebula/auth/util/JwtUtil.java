package com.nebula.auth.util;

import com.nebula.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
@Component
@Setter
public class JwtUtil {

    /**
     * JWT 密钥
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Token过期时间（毫秒）
     */
    @Value("${jwt.expire}")
    private Long expire;

    /**
     * JWT Key
     */
//    private Key key;
    private SecretKey key;

    /**
     * 初始化 Key
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token
     *
     * @param user 用户
     * @return JWT Token
     */
    public String createToken(User user) {

        Date now = new Date();

        Date expireDate = new Date(now.getTime() + expire);

        return Jwts.builder()
                // 用户唯一标识
                .subject(String.valueOf(user.getId()))
                // 签发时间
                .issuedAt(now)
                // 过期时间
                .expiration(expireDate)
                // 签名
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
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