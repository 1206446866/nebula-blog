package com.nebula.auth.service;

import com.nebula.auth.security.TokenSessionUnavailableException;
import com.nebula.auth.vo.DeviceSessionVO;
import com.nebula.user.entity.User;
import cn.hutool.http.useragent.UserAgentUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** 管理 Redis 中尚未到期的会话和本人可见的设备记录。 */
@Service
@RequiredArgsConstructor
public class TokenSessionService {
    private static final String PREFIX = "auth:session:";
    private static final String DEVICES_PREFIX = "auth:session-devices:";
    private static final DefaultRedisScript<Long> REGISTER_DEVICE = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end
            redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', ARGV[1])
            redis.call('ZADD', KEYS[2], ARGV[2], ARGV[3])
            local latest = redis.call('ZREVRANGE', KEYS[2], 0, 0, 'WITHSCORES')
            redis.call('PEXPIREAT', KEYS[2], latest[2])
            redis.call('SET', KEYS[1], ARGV[4], 'PXAT', ARGV[2])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> REVOKE_DEVICE = new DefaultRedisScript<>("""
            if not redis.call('ZSCORE', KEYS[2], ARGV[1]) then return 0 end
            local deleted = redis.call('DEL', KEYS[1])
            redis.call('ZREM', KEYS[2], ARGV[1])
            return deleted
            """, Long.class);
    private final StringRedisTemplate redis;

    /** 原子登记会话和设备索引，只保留浏览器与系统的识别结果。 */
    public void register(String sessionId, User user, Instant signedInAt, Instant expiresAt, String userAgent) {
        if (!expiresAt.isAfter(Instant.now())) throw new TokenSessionUnavailableException();
        String metadata = binding(user) + '\n' + signedInAt.toEpochMilli() + '\n' + expiresAt.toEpochMilli()
                + '\n' + describeDevice(userAgent);
        try {
            Long result = redis.execute(REGISTER_DEVICE, List.of(key(sessionId), devicesKey(user)),
                    Long.toString(System.currentTimeMillis()), Long.toString(expiresAt.toEpochMilli()),
                    sessionId, metadata);
            if (!Long.valueOf(1).equals(result)) throw new TokenSessionUnavailableException();
        } catch (DataAccessException e) {
            throw new TokenSessionUnavailableException(e);
        }
    }

    public boolean isActive(String sessionId, User currentUser) {
        if (!validId(sessionId) || currentUser.getPassword() == null) return false;
        try {
            String expected = binding(currentUser);
            String stored = redis.opsForValue().get(key(sessionId));
            return expected.equals(stored) || (stored != null && stored.startsWith(expected + '\n'));
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

    /** 仅查询当前账号版本下未过期且仍存在的设备会话。 */
    public List<DeviceSessionVO> listDevices(User user, String currentSessionId) {
        try {
            long now = System.currentTimeMillis();
            var ids = redis.opsForZSet().reverseRangeByScore(devicesKey(user), now + 1, Double.POSITIVE_INFINITY);
            if (ids == null) throw new TokenSessionUnavailableException();
            var sessionIds = ids.stream().filter(this::validId).toList();
            if (sessionIds.isEmpty()) return List.of();
            var stored = redis.opsForValue().multiGet(sessionIds.stream().map(this::key).toList());
            if (stored == null || stored.size() != sessionIds.size()) throw new TokenSessionUnavailableException();
            var result = new ArrayList<DeviceSessionVO>();
            String expected = binding(user);
            for (int i = 0; i < sessionIds.size(); i++) {
                String id = sessionIds.get(i);
                String value = stored.get(i);
                if (value == null || !value.startsWith(expected + '\n')) continue;
                String[] fields = value.split("\n", -1);
                if (fields.length != 5) continue;
                result.add(new DeviceSessionVO(id, fields[3], fields[4], Instant.ofEpochMilli(Long.parseLong(fields[1])),
                        Instant.ofEpochMilli(Long.parseLong(fields[2])), id.equals(currentSessionId)));
            }
            result.sort(Comparator.comparing(DeviceSessionVO::signedInAt).reversed());
            return result;
        } catch (DataAccessException | NumberFormatException e) {
            throw new TokenSessionUnavailableException(e);
        }
    }

    /** 设备索引同时限定账号与版本，只有索引中的会话可被指定撤销。 */
    public boolean revokeDevice(User user, String sessionId) {
        if (!validId(sessionId)) return false;
        try {
            Long result = redis.execute(REVOKE_DEVICE, List.of(key(sessionId), devicesKey(user)), sessionId);
            if (result == null) throw new TokenSessionUnavailableException();
            return result == 1;
        } catch (DataAccessException e) {
            throw new TokenSessionUnavailableException(e);
        }
    }

    /** 只解析有限长度的客户端标头，解析失败时展示未知设备。 */
    private String describeDevice(String userAgent) {
        if (userAgent != null && !userAgent.isBlank()) {
            try {
                var agent = UserAgentUtil.parse(userAgent.substring(0, Math.min(512, userAgent.length())));
                if (agent != null && agent.getBrowser() != null && agent.getOs() != null) {
                    return agent.getBrowser().getName() + '\n' + agent.getOs().getName();
                }
            } catch (RuntimeException ignored) {
                // 标头异常不影响登录。
            }
        }
        return "未知浏览器\n未知系统";
    }

    /** 版本切换后旧索引自然过期，新会话使用新索引。 */
    private String devicesKey(User user) {
        return DEVICES_PREFIX + user.getId() + ':' + user.currentSessionVersion();
    }

    /** 用用户ID和当前密码摘要绑定会话，使修改密码后原会话自动失效。 */
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

    /** 只接受标准UUID会话标识，拒绝旧Token或畸形会话ID。 */
    private boolean validId(String id) {
        if (id == null) return false;
        try { return UUID.fromString(id).toString().equals(id); }
        catch (IllegalArgumentException e) { return false; }
    }

    /** 为已验证的会话ID添加统一Redis键前缀。 */
    private String key(String sessionId) { return PREFIX + sessionId; }
}
