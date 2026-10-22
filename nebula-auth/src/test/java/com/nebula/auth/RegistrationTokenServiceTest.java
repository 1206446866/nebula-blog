package com.nebula.auth;

import com.nebula.auth.dto.RegisterCacheDTO;
import com.nebula.auth.security.RegistrationUnavailableException;
import com.nebula.auth.service.RegistrationTokenService;
import com.nebula.common.constant.RegisterType;
import com.nebula.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class RegistrationTokenServiceTest {
    private final RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
    private final ValueOperations<String, Object> values = mock(ValueOperations.class);
    private final RegistrationTokenService credentials = new RegistrationTokenService(redis, new com.nebula.auth.security.AuthAbuseProperties());
    private final String token = "0123456789abcdef0123456789abcdef";
    private final String key = "auth:register:token:" + token;
    private final RegisterCacheDTO cache = new RegisterCacheDTO();

    @BeforeEach
    void setup() {
        cache.setAccount("fixture@example.invalid");
        cache.setPassword("stored-bcrypt-hash");
        cache.setType(RegisterType.EMAIL);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.getExpire(key, TimeUnit.MILLISECONDS)).thenReturn(600_000L);
        when(values.getAndDelete(key)).thenReturn(cache);
    }

    @AfterEach
    void cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private void beginTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    @Test
    void redisReadAndDeserializationFailuresDoNotBecomeInvalidLinkOrSuccess() {
        when(redis.getExpire(key, TimeUnit.MILLISECONDS)).thenThrow(new DataAccessResourceFailureException("outage"));
        assertThrows(RegistrationUnavailableException.class, () -> credentials.consume(token));
        doReturn(600_000L).when(redis).getExpire(key, TimeUnit.MILLISECONDS);
        when(values.getAndDelete(key)).thenThrow(new SerializationException("invalid stored payload"));
        assertThrows(RegistrationUnavailableException.class, () -> credentials.consume(token));
    }

    @Test
    void unknownTtlCannotIssueAnUnboundedCredential() {
        when(redis.getExpire(key, TimeUnit.MILLISECONDS)).thenReturn(null);
        assertThrows(RegistrationUnavailableException.class, () -> credentials.consume(token));
        verify(values, never()).getAndDelete(anyString());
    }

    @ParameterizedTest
    @ValueSource(longs = {-2, -1, 0})
    void missingExpiredAndUnboundedCredentialsAreRejected(long ttl) {
        when(redis.getExpire(key, TimeUnit.MILLISECONDS)).thenReturn(ttl);
        assertEquals(40011, assertThrows(BusinessException.class, () -> credentials.consume(token)).getCode());
        verify(values, never()).getAndDelete(anyString());
    }

    @Test
    void rollbackRestoresOnlyTheRemainingLifetimeAndDoesNotOverwriteAnotherRecord() {
        beginTransaction();
        assertSame(cache, credentials.consume(token));
        TransactionSynchronizationManager.getSynchronizations().forEach(
                sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        var ttl = ArgumentCaptor.forClass(Duration.class);
        verify(values).setIfAbsent(eq(key), same(cache), ttl.capture());
        assertTrue(ttl.getValue().toMillis() > 590_000 && ttl.getValue().toMillis() <= 600_000);
    }

    @ParameterizedTest
    @ValueSource(ints = {TransactionSynchronization.STATUS_COMMITTED, TransactionSynchronization.STATUS_UNKNOWN})
    void successfulOrUncertainTransactionsNeverRestoreTheLink(int status) {
        beginTransaction();
        credentials.consume(token);
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(status));
        verify(values, never()).setIfAbsent(anyString(), any(), any(Duration.class));
    }

    @Test
    void failedRollbackRestorationDoesNotExposeCredentialsOrMaskTheOriginalFailure() {
        beginTransaction();
        credentials.consume(token);
        when(values.setIfAbsent(anyString(), any(), any(Duration.class)))
                .thenThrow(new DataAccessResourceFailureException("outage"));
        assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations().forEach(
                sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)));
    }

    @Test
    void captchaPrecheckFailsClosedAndWrongCodesDoNotConsumeTheValidCode() {
        doReturn(1L).when(redis).execute(any(RedisScript.class), anyList(), any(), any());
        assertDoesNotThrow(() -> credentials.requireCaptcha("fixture@example.invalid", "123456"));
        doReturn(0L).when(redis).execute(any(RedisScript.class), anyList(), any(), any());
        assertEquals(40010, assertThrows(BusinessException.class,
                () -> credentials.requireCaptcha("fixture@example.invalid", "wrong")).getCode());
        verify(values, never()).getAndDelete(anyString());
        doThrow(new DataAccessResourceFailureException("outage")).when(redis).execute(any(RedisScript.class), anyList(), any(), any());
        assertThrows(RegistrationUnavailableException.class,
                () -> credentials.requireCaptcha("fixture@example.invalid", "123456"));
    }

    @Test
    void atomicIssuanceFailuresDoNotReportVerificationSuccess() {
        doReturn(0L).when(redis).execute(any(RedisScript.class), anyList(), any(), any(), any(), any());
        assertEquals(40010, assertThrows(BusinessException.class,
                () -> credentials.verifyAndIssue("fixture@example.invalid", "123456", "hash")).getCode());
        doThrow(new DataAccessResourceFailureException("outage")).when(redis)
                .execute(any(RedisScript.class), anyList(), any(), any(), any(), any());
        assertThrows(RegistrationUnavailableException.class,
                () -> credentials.verifyAndIssue("fixture@example.invalid", "123456", "hash"));
    }
}
