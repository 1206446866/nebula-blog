package com.nebula.start;

import com.nebula.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 超时应按可重试的临时不可用返回，不能因触发超时的阶段不同变成内部错误。 */
class SearchTimeoutResponseTest {
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new TimeoutController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void statementTimeoutReturnsTemporaryUnavailability() throws Exception {
        mvc.perform(get("/timeout-probe/statement"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void exhaustedTransactionDeadlineAlsoReturnsTemporaryUnavailability() throws Exception {
        mvc.perform(get("/timeout-probe/transaction"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void persistenceWrappedDeadlineStillReturnsRetryableResponseWithoutCaching() throws Exception {
        mvc.perform(get("/timeout-probe/wrapped"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void unrelatedInternalFailuresRemainInternalErrors() throws Exception {
        mvc.perform(get("/timeout-probe/unrelated"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务器异常，请稍后重试"));
    }

    @RestController
    static class TimeoutController {
        @GetMapping("/timeout-probe/statement")
        public void statement() { throw new QueryTimeoutException("隔离SQL超时探针"); }

        @GetMapping("/timeout-probe/transaction")
        public void transaction() { throw new TransactionTimedOutException("隔离事务期限探针"); }

        @GetMapping("/timeout-probe/wrapped")
        public void wrapped() { throw new org.mybatis.spring.MyBatisSystemException(new TransactionTimedOutException("隔离包装超时探针")); }

        @GetMapping("/timeout-probe/unrelated")
        public void unrelated() { throw new IllegalStateException("内部诊断不得返回客户端"); }
    }
}
