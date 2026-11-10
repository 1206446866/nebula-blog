package com.nebula.common.security;

import com.nebula.common.exception.DatabaseUnavailable;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import static org.junit.jupiter.api.Assertions.*;

/** 验证嵌套预算恢复、异常释放和数据库错误分类边界。 */
class DatabaseReadBudgetTest {
    @Test void nestedBudgetCannotExtendCallerAndClosingRestoresThread() {
        assertNull(DatabaseReadBudget.remainingSeconds());
        try (var outer = DatabaseReadBudget.open(1)) {
            try (var inner = DatabaseReadBudget.open(30)) { assertEquals(1, DatabaseReadBudget.remainingSeconds()); }
            assertEquals(1, DatabaseReadBudget.remainingSeconds());
        }
        assertNull(DatabaseReadBudget.remainingSeconds());
    }

    @Test void expiredBudgetFailsBeforeAnotherQueryAndExceptionStillRestoresCaller() {
        try (var outer = DatabaseReadBudget.open(5)) {
            assertThrows(QueryTimeoutException.class, () -> {
                try (var inner = DatabaseReadBudget.open(0)) { DatabaseReadBudget.remainingSeconds(); }
            });
            assertTrue(DatabaseReadBudget.remainingSeconds() > 0);
        }
        assertNull(DatabaseReadBudget.remainingSeconds());
    }

    @Test void onlyConnectionAndTimeoutFailuresAreRetryable() {
        assertTrue(DatabaseUnavailable.matches(new RuntimeException(new java.sql.SQLTimeoutException())));
        assertTrue(DatabaseUnavailable.matches(new java.sql.SQLException("断开", "08S01")));
        assertFalse(DatabaseUnavailable.matches(new java.sql.SQLException("语法", "42000")));
        assertFalse(DatabaseUnavailable.matches(new org.springframework.dao.DuplicateKeyException("重复")));
        var cycle = new RuntimeException(); var other = new RuntimeException(cycle); cycle.initCause(other);
        assertFalse(DatabaseUnavailable.matches(cycle));
    }
}
