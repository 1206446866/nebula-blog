package com.nebula.common.exception;

import com.nebula.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 全局异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("请求处理失败", e);
        return error(500, 500, "服务器异常，请稍后重试");
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Result<Void>> handleBaseException(BaseException e) {
        if (e.getHttpStatus() >= 500) log.error("业务操作失败", e);
        return error(e.getHttpStatus(), e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleValidException(BindException e) {
        if (e.getFieldErrors().stream().anyMatch(org.springframework.validation.FieldError::isBindingFailure)) {
            return error(400, 400, "请求格式或参数类型错误");
        }
        String message = e.getAllErrors().stream().findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage).orElse("请求参数校验失败");
        return error(400, 400, message);
    }

    /**
     * 参数异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream().findFirst()
                .map(violation -> violation.getMessage()).orElse("请求参数校验失败");
        return error(400, 400, message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Result<Void>> handleMethodValidation(HandlerMethodValidationException e) {
        if (e.isForReturnValue()) return handleException(e);
        return error(400, 400, "请求参数校验失败");
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAuthorizationDeniedException(AccessDeniedException e) {
        return error(403, 403, "权限不足");
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Result<Void>> handleAuthException(AuthException e) {
        return error(401, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(AuthenticationException e) {
        return error(401, 401, "未登录或登录状态已失效，请重新登录");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<Result<Void>> handleMalformedInput(Exception e) {
        return error(400, 400, "请求格式或参数类型错误");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<Result<Void>> handleMissingInput(Exception e) {
        return error(400, 400, "缺少必要参数");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleOversizedUpload(MaxUploadSizeExceededException e) {
        return error(413, 413, "上传文件过大");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Result<Void>> handleMalformedUpload(MultipartException e) {
        return error(400, 400, "上传请求格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleUnsupportedMethod(HttpRequestMethodNotSupportedException e) {
        var response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).contentType(MediaType.APPLICATION_JSON);
        if (e.getSupportedHttpMethods() != null) response.allow(e.getSupportedHttpMethods().toArray(org.springframework.http.HttpMethod[]::new));
        return response.body(Result.error(405, "请求方法不支持"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        return error(415, 415, "请求内容类型不支持");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Result<Void>> handleUnacceptableMediaType(HttpMediaTypeNotAcceptableException e) {
        return error(406, 406, "请求的响应类型不支持");
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Result<Void>> handleMissingResource(Exception e) {
        return error(404, 404, "请求的资源不存在");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataConflict(DataIntegrityViolationException e) {
        log.warn("数据写入冲突", e);
        return error(409, 409, "数据冲突，请检查后重试");
    }

    private ResponseEntity<Result<Void>> error(int status, int code, String message) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(Result.error(code, message));
    }

}
