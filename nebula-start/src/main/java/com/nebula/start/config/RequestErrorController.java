package com.nebula.start.config;

import com.nebula.common.result.Result;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 容器读取超时等错误使用同一公开响应，不返回堆栈、异常消息和请求正文。 */
@RestController
@io.swagger.v3.oas.annotations.Hidden
@io.swagger.v3.oas.annotations.tags.Tag(name = "容器错误", description = "请求读取及容器错误的公开提示")
public class RequestErrorController implements ErrorController {
    @RequestMapping("${server.error.path:${error.path:/error}}")
    @io.swagger.v3.oas.annotations.Operation(summary = "返回公开错误", description = "容器错误转发保留原状态；直接访问仍需认证，不返回内部异常内容")
    public ResponseEntity<Result<Void>> error(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = value instanceof Integer code && code >= 400 && code <= 599 ? code : 500;
        String message = switch (status) {
            case 400 -> "请求格式错误";
            case 404 -> "请求的接口不存在";
            case 405 -> "请求方法不支持";
            case 408 -> "请求正文读取超时，请重新发送";
            case 413 -> "请求正文过大";
            default -> "请求处理失败，请稍后重试";
        };
        return ResponseEntity.status(status).header("Cache-Control", "no-store")
                .body(Result.error(status, message));
    }
}
