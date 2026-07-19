package com.nebula.common.exception;

import com.nebula.common.exception.code.ErrorCode;
import lombok.Getter;

@Getter
public class AuthException extends BaseException {
    /**
     * 错误码
     */
    private final Integer code;

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
        this.code = errorCode.getCode();
    }
}
