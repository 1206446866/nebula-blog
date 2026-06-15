package com.nebula.common.exception;

import com.nebula.common.exception.code.ErrorCode;

public class BusinessException extends BaseException{
    /**
     * 错误码
     */
    private final Integer code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
        this.code = errorCode.getCode();
    }
}
