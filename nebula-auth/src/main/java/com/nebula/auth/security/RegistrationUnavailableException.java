package com.nebula.auth.security;

public class RegistrationUnavailableException extends RuntimeException {
    public RegistrationUnavailableException() { super("注册服务暂时不可用，请稍后重试"); }
    public RegistrationUnavailableException(Throwable cause) { super("注册服务暂时不可用，请稍后重试", cause); }
}
