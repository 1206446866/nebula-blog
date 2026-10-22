package com.nebula.common.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RegistrationErrorCode implements ErrorCode {
    CAPTCHA_INVALID(40010, "验证码错误或已失效，请重新获取"),
    REGISTRATION_LINK_INVALID(40011, "注册链接无效或已使用，请重新验证邮箱"),
    CAPTCHA_ATTEMPTS_EXCEEDED(40012, "验证码错误次数过多，请重新获取"),
    EMAIL_ALREADY_REGISTERED(40910, "该邮箱已注册，请直接登录");

    private final Integer code;
    private final String message;

    @Override
    public int getHttpStatus() {
        return this == EMAIL_ALREADY_REGISTERED ? 409 : 400;
    }
}
