package com.nebula.common.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    USER_NOT_FOUND(40101, "用户不存在"),
    PASSWORD_ERROR(40102, "密码错误"),
    TOKEN_INVALID(40103, "Token无效"),
    TOKEN_EXPIRED(40104, "Token已过期");
    private final Integer code;
    private final String message;
}
