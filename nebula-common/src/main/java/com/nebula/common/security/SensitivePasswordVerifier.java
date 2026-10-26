package com.nebula.common.security;

import com.nebula.common.exception.ResourceLimitException;
import com.nebula.common.util.ClientIpResolver;
import com.nebula.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** 敏感操作共用操作者账号预算，与匿名登录尝试计数隔离。 */
@Service
@RequiredArgsConstructor
public class SensitivePasswordVerifier {
    private final StringRedisTemplate redis;
    private final PasswordEncoder passwords;
    private final ClientIpResolver ips;
    private final SensitivePasswordProperties limits;
    private static final DefaultRedisScript<Long> ACQUIRE = new DefaultRedisScript<>("""
            local count = tonumber(redis.call('GET', KEYS[1]) or '0')
            if count >= tonumber(ARGV[1]) then
                local ttl = redis.call('PTTL', KEYS[1])
                if ttl < 1 then redis.call('PEXPIRE', KEYS[1], ARGV[2]); ttl = tonumber(ARGV[2]) end
                return math.ceil(ttl / 1000)
            end
            local next = redis.call('INCR', KEYS[1])
            if next == 1 or next == tonumber(ARGV[1]) or redis.call('PTTL', KEYS[1]) < 1 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end
            return 0
            """, Long.class);

    /**
     * 验证操作者的当前密码，先原子领取账号预算再计算哈希；失败由各业务返回原有提示。
     * @param rawPassword 当前密码原文，允许历史短密码，不做裁剪或新密码长度校验
     * @param encodedPassword 业务从可信数据库或认证主体取得的操作者密码哈希
     * @return 密码是否匹配；匹配成功清除账号失败预算，IP预算不会因此清除
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (SecurityUtils.getLoginUser() == null) throw new AuthenticationCredentialsNotFoundException("请先登录");
        String account = limits.getKeyPrefix() + "account:" + SecurityUtils.getUserId();
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes request)
                acquire(limits.getKeyPrefix() + "ip:" + digest(ips.getIp(request.getRequest())), limits.getIpRequests(), limits.getIpWindowSeconds());
            acquire(account, limits.getAttempts(), limits.getWindowSeconds());
            boolean valid = false;
            try { valid = rawPassword != null && encodedPassword != null && passwords.matches(rawPassword, encodedPassword); }
            catch (IllegalArgumentException ignored) { /* 超出编码器可处理范围按验证失败计数，不输出原文。 */ }
            if (valid) redis.delete(account);
            return valid;
        } catch (DataAccessException | SerializationException e) { throw unavailable(); }
    }
    /** 使用Redis原子操作限制并发尝试，达到上限后的等待时间从最后一笔预算预留起计算。 */
    private void acquire(String key, int attempts, int seconds) {
        Long wait = redis.execute(ACQUIRE, List.of(key), String.valueOf(attempts), String.valueOf(seconds * 1000L));
        if (wait == null) throw unavailable();
        if (wait > 0) throw new ResourceLimitException(429, wait, "密码验证失败次数较多，请稍后重试");
    }
    /** 来源IP只保存摘要，避免Redis键保留明文来源信息。 */
    private String digest(String source) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    /** 保护设施不可用时拒绝敏感操作，避免退化为无限猜测。 */
    private ResourceLimitException unavailable() { return new ResourceLimitException(503, 5, "密码验证保护暂不可用，请稍后重试"); }
}
