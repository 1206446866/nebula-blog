package com.nebula.common.constant;


import com.nebula.common.exception.code.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum LoginStatus implements ErrorCode {

    FAIL(0, "登录失败"),

    /**
     * 登录成功
     */
    SUCCESS(1, "登录成功"),

    /**
     * 用户不存在
     */
    USER_NOT_FOUND(2, "用户不存在"),

    /**
     * 密码错误
     */
    PASSWORD_ERROR(3, "密码错误"),

    /**
     * 用户已禁用
     */
    USER_DISABLED(4, "用户已禁用"),

    /**
     * Token已失效
     */
    TOKEN_EXPIRED(5, "Token已失效"),

    /**
     * Token无效
     */
    TOKEN_INVALID(6, "Token无效"),

    /**
     * LOCKED 锁定
     */
    USER_LOCKED(7,"用户已锁定"),

    /**
     * 未知原因
     */
    UNKNOWN(99, "未知原因");

    private final Integer code;

    private final String desc;

    /**
     * 根据状态码获取枚举
     */
    public static LoginStatus fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知登录状态：" + code));
    }

    @Override
    public String getMessage() {
        return this.desc;
    }
}
