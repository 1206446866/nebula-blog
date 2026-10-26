package com.nebula.common.security;

import com.nebula.common.exception.ResourceLimitException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Redis 原子频率预算与本实例工作量上限，不依赖客户端自行节流。 */
@Service
public class ResourceGuard {
    public enum Work { UPLOAD, SEARCH, READ, WRITE, DECODE }
    private static final DefaultRedisScript<Long> ACQUIRE = new DefaultRedisScript<>("""
            local wait = 0
            for i,key in ipairs(KEYS) do
                local count = tonumber(redis.call('GET', key) or '0')
                if count >= tonumber(ARGV[i + 1]) then
                    local ttl = redis.call('PTTL', key)
                    if ttl < 1 then redis.call('PEXPIRE', key, ARGV[1]); ttl = tonumber(ARGV[1]) end
                    wait = math.max(wait, math.ceil(ttl / 1000))
                end
            end
            if wait > 0 then return wait end
            for i,key in ipairs(KEYS) do
                local count = redis.call('INCR', key)
                if count == 1 or redis.call('PTTL', key) < 1 then redis.call('PEXPIRE', key, ARGV[1]) end
            end
            return 0
            """, Long.class);
    private final StringRedisTemplate redis;
    private final ResourceLimitProperties properties;
    private final Map<Work, Semaphore> slots = new EnumMap<>(Work.class);
    private final Semaphore totalSlots;
    private final Map<String, Integer> activeScopes = new ConcurrentHashMap<>();

    public ResourceGuard(StringRedisTemplate redis, ResourceLimitProperties properties) {
        this.redis = redis; this.properties = properties;
        this.totalSlots = new Semaphore(properties.getMaxConcurrency());
        slots.put(Work.UPLOAD, new Semaphore(properties.getUpload().getConcurrency()));
        slots.put(Work.SEARCH, new Semaphore(properties.getSearch().getConcurrency()));
        slots.put(Work.READ, new Semaphore(properties.getRead().getConcurrency()));
        slots.put(Work.WRITE, new Semaphore(properties.getWrite().getConcurrency()));
        slots.put(Work.DECODE, new Semaphore(properties.getDecodeConcurrency()));
    }

    /**
     * 同时消耗账号和可信IP的预算，并领取本实例工作名额；超限或Redis异常时拒绝放行。
     * @param work 上传、搜索、图片读取或业务写入入口
     * @param ip 服务端解析的可信来源IP
     * @param userId 当前认证账号；匿名图片请求传空，只使用IP预算
     * @return 必须在请求完成或异常时关闭的工作名额
     */
    public Permit enter(Work work, String ip, Long userId) {
        var rate = switch (work) {
            case UPLOAD -> properties.getUpload();
            case SEARCH -> properties.getSearch();
            case READ -> properties.getRead();
            case WRITE -> properties.getWrite();
            default -> throw new IllegalArgumentException("解码只能领取本地工作名额");
        };
        List<String> keys = new ArrayList<>(List.of(key(work, "ip", ip)));
        List<String> args = new ArrayList<>(List.of(String.valueOf(properties.getWindowSeconds() * 1000L), String.valueOf(rate.getIp())));
        if (userId != null) { keys.add(key(work, "account", userId.toString())); args.add(String.valueOf(rate.getAccount())); }
        try {
            Long wait = redis.execute(ACQUIRE, keys, args.toArray());
            if (wait == null) throw unavailable();
            if (wait > 0) throw new ResourceLimitException(429, wait, "操作过于频繁，请稍后重试");
        } catch (DataAccessException | SerializationException e) { throw unavailable(); }
        Permit total = permit(totalSlots);
        Permit global = null;
        Permit ipPermit = null;
        try {
            global = acquire(work);
            ipPermit = acquireScope(keys.get(0), rate.getIpConcurrency());
            // 名额所有权交给返回的Permit，正常路径由调用方关闭，不能在这里提前释放。
            Permit account = userId == null ? () -> {} : acquireScope(keys.get(1), rate.getAccountConcurrency());
            Permit source = ipPermit;
            Permit instance = global;
            return () -> { account.close(); source.close(); instance.close(); total.close(); };
        } catch (RuntimeException e) {
            if (ipPermit != null) ipPermit.close();
            if (global != null) global.close();
            total.close();
            throw e;
        }
    }

    /**
     * 领取本实例工作名额，不排队堆积图片或搜索请求。
     * @param work 需要限制并发的工作类型
     * @return 可重复关闭且只释放一次的工作名额
     */
    public Permit acquire(Work work) {
        return permit(slots.get(work));
    }

    /** 非阻塞领取信号量，异常路径和重复关闭均不会泄漏或重复归还名额。 */
    private Permit permit(Semaphore semaphore) {
        if (!semaphore.tryAcquire()) throw new ResourceLimitException(429, 1, "当前处理请求较多，请稍后重试");
        var closed = new AtomicBoolean();
        return () -> { if (closed.compareAndSet(false, true)) semaphore.release(); };
    }

    /** 原子领取账号或IP的并发名额；归零即移除，计数键数量受全局并发上限约束。 */
    private Permit acquireScope(String key, int limit) {
        var accepted = new AtomicBoolean();
        activeScopes.compute(key, (ignored, count) -> {
            int current = count == null ? 0 : count;
            if (current >= limit) return count;
            accepted.set(true);
            return current + 1;
        });
        if (!accepted.get()) throw new ResourceLimitException(429, 1, "当前来源或账号的请求较多，请稍后重试");
        var closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) activeScopes.computeIfPresent(key, (ignored, count) -> count <= 1 ? null : count - 1);
        };
    }

    /** 使用摘要隔离IP与账号键，避免在Redis键中保存明文来源信息。 */
    private String key(Work work, String scope, String subject) {
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(subject.getBytes(StandardCharsets.UTF_8)));
            return properties.getKeyPrefix() + work.name().toLowerCase(Locale.ROOT) + ":" + scope + ":" + digest;
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    /** 保护设施异常时只拒绝高成本请求，不退化为无限制运行。 */
    private ResourceLimitException unavailable() { return new ResourceLimitException(503, 5, "资源保护暂不可用，请稍后重试"); }

    @FunctionalInterface
    public interface Permit extends AutoCloseable {
        /** 释放当前名额，重复调用不会增加可用名额。 */
        @Override void close();
    }
}
