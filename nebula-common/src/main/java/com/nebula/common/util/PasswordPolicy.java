package com.nebula.common.util;

import com.nebula.common.exception.BusinessException;
import com.nebula.common.exception.code.AuthErrorCode;
import java.nio.charset.StandardCharsets;

/** 仅检测新设置的密码，不用于登录或旧密码确认，不修改密码原文。 */
public final class PasswordPolicy {
    public static final String MESSAGE = "新密码需为6至64个字符且不超过72字节";
    private PasswordPolicy() {}
    /** 按Unicode码点数和UTF-8字节数检查新密码，不设置常见密码黑名单。 */
    public static boolean isValidNewPassword(String password) {
        if (password == null || password.isBlank()) return false;
        int length = password.codePointCount(0, password.length());
        return length >= 6 && length <= 64 && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
    /** 新密码不满足规则时抛出统一业务错误；Service直接调用同样受到校验。 */
    public static void validateNewPassword(String password) {
        if (!isValidNewPassword(password)) throw new BusinessException(AuthErrorCode.PASSWORD_POLICY_INVALID);
    }
}
