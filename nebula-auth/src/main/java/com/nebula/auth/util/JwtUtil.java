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
     * 密钥
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * 过期时间（毫秒）
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

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token
     */
    public String generateToken(Long userId, String username, String role) {

        Date now = new Date();

        Date expireDate = new Date(now.getTime() + expire);

        return Jwts.builder().setSubject(username).claim("userId", userId).claim("role", role).setIssuedAt(now).setExpiration(expireDate).signWith(key, SignatureAlgorithm.HS256).compact();
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {

        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
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
     * 获取用户名
     */
    public String getUsername(String token) {

        return parseToken(token).getSubject();
    }

    /**
     * 获取用户ID
     */
    public Long getUserId(String token) {

        Object userId = parseToken(token).get("userId");

        return Long.parseLong(userId.toString());
    }

    /**
     * 获取角色
     */
    public String getRole(String token) {

        Object role = parseToken(token).get("role");

        return role.toString();
    }
}