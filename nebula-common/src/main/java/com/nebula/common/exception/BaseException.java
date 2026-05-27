package com.nebula.common.exception;


import com.nebula.common.exception.code.ErrorCode;
import lombok.Getter;


/**
 * 基础业务异常
 */
@Getter
public abstract class BaseException extends RuntimeException {
    /**
     * 错误码
     */
    private final Integer code;

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
