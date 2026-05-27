package com.nebula.common.exception;


/**
 * 基础业务异常
 */
public abstract class BaseException extends RuntimeException {
    public BaseException(String message) {
        super(message);
    }
}
