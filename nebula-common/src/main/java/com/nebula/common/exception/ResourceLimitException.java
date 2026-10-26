package com.nebula.common.exception;

import lombok.Getter;

/** 资源预算不足或保护设施不可用，响应中提供建议等待时间。 */
@Getter
public class ResourceLimitException extends RuntimeException {
    private final int httpStatus;
    private final long retryAfterSeconds;
    public ResourceLimitException(int httpStatus, long retryAfterSeconds, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }
}
