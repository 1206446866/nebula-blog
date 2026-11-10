package com.nebula.common.exception;

/** 只识别数据库连接获取失败和明确超时，不把SQL错误或约束冲突伪装成繁忙。 */
public final class DatabaseUnavailable {
    private DatabaseUnavailable() { }

    public static boolean matches(Throwable error) {
        var visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable, Boolean>());
        while (error != null && visited.add(error)) {
            if (error instanceof org.springframework.jdbc.CannotGetJdbcConnectionException
                    || error instanceof java.sql.SQLTransientConnectionException
                    || error instanceof java.sql.SQLTimeoutException
                    || error instanceof java.sql.SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("08")
                    || error instanceof org.springframework.dao.QueryTimeoutException
                    || error instanceof org.springframework.transaction.TransactionTimedOutException) return true;
            error = error.getCause();
        }
        return false;
    }
}
