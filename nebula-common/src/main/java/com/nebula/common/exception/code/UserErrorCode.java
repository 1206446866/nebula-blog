package com.nebula.common.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode{

    USER_NOT_FOUND(1001, "用户不存在"),

    USER_ROLE_EMPTY(1002, "用户至少需要一个角色"),

    USER_UPDATE_FAILED(1003, "用户更新失败"),

    USER_DELETE_FAILED(1004, "用户删除失败");

    private final Integer code;
    private final String message;
}