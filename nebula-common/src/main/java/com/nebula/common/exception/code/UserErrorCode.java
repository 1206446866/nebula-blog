package com.nebula.common.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode{

    USER_NOT_FOUND(1001, "用户不存在"),

    USER_ROLE_EMPTY(1002, "用户至少需要一个角色"),

    USER_UPDATE_FAILED(1003, "用户更新失败"),

    USER_DELETE_FAILED(1004, "用户删除失败"),

    /**
     * 用户状态修改失败
     */
    USER_STATUS_UPDATE_FAILED(1005, "用户状态修改失败"),

    /**
     * 用户密码修改失败
     */
    USER_PASSWORD_UPDATE_FAILED(1006, "用户密码修改失败"),

    /**
     * 用户无权操作
     */
    USER_ACCESS_DENIED(1007, "无权操作该用户"),

    /**
     * 用户已存在
     */
    USER_ALREADY_EXISTS(1008, "用户已存在"),

    /**
     * 用户已被禁用
     */
    USER_DISABLED(1009, "用户已被禁用");

    private final Integer code;
    private final String message;
}