package com.nebula.common.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    USER_NOT_FOUND(40101, "用户不存在"),
    PASSWORD_ERROR(40102, "密码错误"),
    TOKEN_INVALID(40103, "Token无效"),
    TOKEN_EXPIRED(40104, "Token已过期"),
    OLD_PASSWORD_INCORRECT(40020, "旧密码错误"),
    PASSWORD_CONFIRMATION_MISMATCH(40021, "两次密码不一致"),
    OPERATOR_PASSWORD_INCORRECT(40022, "当前管理员密码错误"),
    PASSWORD_POLICY_INVALID(40023, "新密码需为6至64个字符且不超过72字节"),
    PASSWORD_CHANGE_CONFLICT(40920, "账号或密码已发生变化，请重新登录");
    private final Integer code;
    private final String message;

    @Override
    public int getHttpStatus() {
        return switch (this) {
            case USER_NOT_FOUND -> 404;
            case TOKEN_INVALID, TOKEN_EXPIRED -> 401;
            case PASSWORD_CHANGE_CONFLICT -> 409;
            default -> 400;
        };
    }
}
