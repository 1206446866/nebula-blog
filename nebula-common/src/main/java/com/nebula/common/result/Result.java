package com.nebula.common.result;

import lombok.Data;

@Data
public class Result<T> {
    private int code;     // 200 成功，其他失败
    private String msg;   // 消息提示
    private T data;       // 返回数据

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static Result<Void> error(String msg) {
        Result<Void> r = new Result<>();
        r.code = 500;
        r.msg = msg;
        r.data = null;
        return r;
    }
}