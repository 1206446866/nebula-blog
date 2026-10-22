package com.nebula.auth.security;

import com.nebula.common.result.Result;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthProtectionExceptionHandler {
    @ExceptionHandler(AuthRateLimitException.class)
    public ResponseEntity<Result<Void>> limited(AuthRateLimitException e) {
        return ResponseEntity.status(429).header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()))
                .body(Result.error(429, e.getMessage()));
    }

    @ExceptionHandler(AuthProtectionUnavailableException.class)
    public ResponseEntity<Result<Void>> unavailable(AuthProtectionUnavailableException e) {
        return ResponseEntity.status(503).body(Result.error(503, e.getMessage()));
    }
}
