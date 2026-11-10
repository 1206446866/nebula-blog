package com.nebula.start.config;

import com.nebula.common.security.DatabaseReadBudget;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;
import java.sql.Connection;

/** 将认证和HTTP读取的剩余时间传给MyBatis，保留更短的事务期限。 */
@Component
@Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
public class DatabaseReadBudgetInterceptor implements Interceptor {
    @Override public Object intercept(Invocation invocation) throws Throwable {
        Integer remaining = DatabaseReadBudget.remainingSeconds();
        if (remaining != null) {
            Integer transaction = (Integer) invocation.getArgs()[1];
            invocation.getArgs()[1] = transaction == null ? remaining : Math.min(transaction, remaining);
        }
        return invocation.proceed();
    }
}
