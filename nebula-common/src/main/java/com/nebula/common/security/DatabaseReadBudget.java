package com.nebula.common.security;

import org.springframework.dao.QueryTimeoutException;

/** 同步读取和认证共享单调时钟预算；不在写事务提交后追认超时。 */
public final class DatabaseReadBudget implements AutoCloseable {
    private static final ThreadLocal<Long> DEADLINE = new ThreadLocal<>();
    private final Long previous;

    private DatabaseReadBudget(int seconds) {
        previous = DEADLINE.get();
        long deadline = System.nanoTime() + seconds * 1_000_000_000L;
        DEADLINE.set(previous == null ? deadline : Math.min(previous, deadline));
    }

    /** 嵌套调用只能缩短预算，关闭时恢复调用方的截止时间。 */
    public static DatabaseReadBudget open(int seconds) { return new DatabaseReadBudget(seconds); }

    /** 返回JDBC秒粒度的剩余时间；没有请求预算的后台任务保持原有事务设置。 */
    public static Integer remainingSeconds() {
        Long deadline = DEADLINE.get();
        if (deadline == null) return null;
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) throw new QueryTimeoutException("请求读取预算已耗尽");
        return (int) Math.max(1, (remaining + 999_999_999L) / 1_000_000_000L);
    }

    @Override public void close() {
        if (previous == null) DEADLINE.remove(); else DEADLINE.set(previous);
    }
}
