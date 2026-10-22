package com.nebula.auth.security;

/** A session store outage must fail closed, with a safe, retryable response. */
public class TokenSessionUnavailableException extends RuntimeException {
    public TokenSessionUnavailableException() { super("登录服务暂时不可用，请稍后重试"); }
    public TokenSessionUnavailableException(Throwable cause) { super("登录服务暂时不可用，请稍后重试", cause); }
}
