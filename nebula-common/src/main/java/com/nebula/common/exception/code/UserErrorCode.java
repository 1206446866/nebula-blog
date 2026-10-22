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
    USER_DISABLED(1009, "用户已被禁用"),

    /**
     * 不能对自己进行操作
     */
    USER_SELF_OPERATION_DENIED(1010, "不能对自己进行操作"),

    ACCOUNT_CANCELLATION_UNCONFIRMED(1011, "请确认注销账号"),
    AVATAR_TYPE_INVALID(1012, "请选择支持的图片格式"),
    AVATAR_FILE_REQUIRED(1013, "请选择文件"),
    AVATAR_TOO_LARGE(1014, "图片不能超过10MB"),
    AVATAR_CONTENT_INVALID(1015, "图片内容非法"),
    USER_STATUS_INVALID(1016, "用户状态非法");

    private final Integer code;
    private final String message;

    @Override
    public int getHttpStatus() {
        return switch (this) {
            case USER_NOT_FOUND -> 404;
            case USER_ACCESS_DENIED, USER_DISABLED, USER_SELF_OPERATION_DENIED -> 403;
            case USER_ALREADY_EXISTS -> 409;
            case USER_UPDATE_FAILED, USER_DELETE_FAILED, USER_STATUS_UPDATE_FAILED, USER_PASSWORD_UPDATE_FAILED -> 500;
            case AVATAR_TOO_LARGE -> 413;
            default -> 400;
        };
    }
}
