package com.nebula.common.exception;

import com.nebula.common.exception.code.ErrorCode;

public class BusinessException extends BaseException {
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }
}
