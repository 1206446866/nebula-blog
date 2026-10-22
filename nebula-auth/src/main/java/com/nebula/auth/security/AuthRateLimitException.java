package com.nebula.auth.security;

import lombok.Getter;

@Getter
public class AuthRateLimitException extends RuntimeException {
    private final long retryAfterSeconds;
    public AuthRateLimitException(long retryAfterSeconds) {
        super("操作过于频繁，请稍后重试");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }
}
