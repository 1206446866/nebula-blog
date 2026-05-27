package com.nebula.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
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
    private Key key;

    /**
     * 初始化 Key
     */
    @PostConstruct
    public void init() {

        this.key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 生成 Token
     *
     * @param userId 用户ID
     * @return JWT Token
     */
    public String createToken(Long userId) {

        Date now = new Date();

        Date expireDate = new Date(
                now.getTime() + expire
        );

        return Jwts.builder()

                // 用户唯一标识
                .setSubject(String.valueOf(userId))

                // 签发时间
                .setIssuedAt(now)

                // 过期时间
                .setExpiration(expireDate)

                // 签名
                .signWith(key, SignatureAlgorithm.HS256)

                .compact();
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
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

        return Long.parseLong(
                parseToken(token).getSubject()
        );
    }
}