package com.nebula.common.exception;

import com.nebula.common.exception.code.ErrorCode;
import lombok.Getter;

@Getter
public class AuthenticationException extends BaseException {
    /**
     * 错误码
     */
    private final Integer code;

    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
        this.code = errorCode.getCode();
    }
}
