package com.nebula.common.exception;

import com.nebula.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

/**
 * 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBaseException(BaseException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidException(MethodArgumentNotValidException e) {
        String message = Optional.ofNullable(e.getBindingResult().getFieldError())
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("参数校验失败");
        return Result.error(message);
    }

    /**
     * 参数异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintException(ConstraintViolationException e) {
        return Result.error(e.getMessage());
    }

    /**
     * 全局异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return Result.error("未知的服务器异常:"+e.getMessage());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public Result<?> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        return Result.error(403, "权限不足");
    }
}