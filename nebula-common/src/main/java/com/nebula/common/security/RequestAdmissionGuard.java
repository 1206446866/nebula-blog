package com.nebula.common.security;

import com.nebula.common.exception.ResourceLimitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

/** 在JWT及正文解析前限制可信来源频率和在途请求，来源表有界，拒绝时不访问外部依赖。 */
@Component
public class RequestAdmissionGuard {
    private final RequestProtectionProperties limits;
    private final LongSupplier clock;
    private final Map<String, Source> sources = new HashMap<>();
    private int active;
    private long lastCleanup;

    @Autowired
    public RequestAdmissionGuard(RequestProtectionProperties limits) {
        this(limits, System::nanoTime);
    }

    /** 测试可注入单调时钟，窗口不受系统时间校准影响。 */
    RequestAdmissionGuard(RequestProtectionProperties limits, LongSupplier clock) {
        this.limits = limits; this.clock = clock; this.lastCleanup = clock.getAsLong();
    }

    /**
     * 领取来源预算及本实例在途名额，包含后续认证、正文解析与同步请求处理。
     * @param ip 服务端从可信连接或代理链解析的来源，不接受客户端账号或Token作为隔离键
     * @return 必须在请求结束时释放的幂等名额；保护表满时拒绝新来源，不能淘汰未过期计数
     */
    public synchronized ResourceGuard.Permit enter(String ip) {
        long now = clock.getAsLong();
        long window = limits.getWindowSeconds() * 1_000_000_000L;
        if (now - lastCleanup >= window) cleanup(now, window);
        Source source = sources.get(ip);
        if (source == null) {
            // 表满时每秒最多额外整理一次，随机来源不能令每笔请求都扫描整张表。
            if (sources.size() >= limits.getMaxSources() && now - lastCleanup >= 1_000_000_000L) cleanup(now, window);
            if (sources.size() >= limits.getMaxSources()) throw new ResourceLimitException(503, 1, "请求保护繁忙，请稍后重试");
            source = new Source(now); sources.put(ip, source);
        }
        source.lastSeen = now;
        if (now - source.started >= window) { source.started = now; source.count = 0; }
        if (source.count >= limits.getIpRequests()) {
            long remaining = window - (now - source.started);
            throw new ResourceLimitException(429, Math.max(1, (remaining + 999_999_999L) / 1_000_000_000L), "请求过于频繁，请稍后重试");
        }
        if (active >= limits.getMaxConcurrency() || source.active >= limits.getIpConcurrency())
            throw new ResourceLimitException(429, 1, "当前请求较多，请稍后重试");
        source.count++; source.active++; active++;
        Source owner = source;
        var closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) release(owner);
        };
    }

    /** 只返还当前调用拥有的并发名额，不清除频率计数。 */
    private synchronized void release(Source owner) { owner.active--; active--; }

    /** 仅移除已空闲一个完整窗口且不持有名额的来源，不能刷新仍有效的频率预算。 */
    private void cleanup(long now, long window) {
        sources.values().removeIf(value -> value.active == 0 && now - value.lastSeen >= window);
        lastCleanup = now;
    }

    /** 来源状态只保存计数和单调时钟，不保留账号或凭据。 */
    private static final class Source {
        private long started, lastSeen;
        private int count, active;
        private Source(long now) { started = now; lastSeen = now; }
    }
}
