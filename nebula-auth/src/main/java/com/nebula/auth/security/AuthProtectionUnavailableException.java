package com.nebula.auth.security;

public class AuthProtectionUnavailableException extends RuntimeException {
    public AuthProtectionUnavailableException() { super("认证安全服务暂时不可用，请稍后重试"); }
    public AuthProtectionUnavailableException(Throwable cause) { super("认证安全服务暂时不可用，请稍后重试", cause); }
}
