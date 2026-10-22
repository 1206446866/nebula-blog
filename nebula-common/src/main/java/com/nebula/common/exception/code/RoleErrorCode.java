package com.nebula.common.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleErrorCode implements ErrorCode {
    ROLE_NOT_FOUND(1101, "角色不存在或已删除"),
    PERMISSION_NOT_FOUND(1102, "包含不存在或已删除的权限"),
    BUILT_IN_ROLE_PROTECTED(1103, "内置角色编码不可修改或删除"),
    SUPER_ADMIN_PERMISSIONS_IMPLICIT(1104, "超级管理员自动拥有全部权限，无需单独分配"),
    INVALID_ROLE_ASSIGNMENT(1105, "包含不存在或已删除的角色"),
    LAST_SUPER_ADMIN_PROTECTED(1106, "请先将管理权限移交给另一位可用的超级管理员"),
    ROLE_NAME_EXISTS(1107, "角色编码已存在"),
    PERMISSION_NAME_INVALID(1108, "必须使用[英文字符+数字]:[英文字符+数字]的格式");

    private final Integer code;
    private final String message;

    @Override
    public int getHttpStatus() {
        return switch (this) {
            case ROLE_NOT_FOUND -> 404;
            case BUILT_IN_ROLE_PROTECTED -> 403;
            case LAST_SUPER_ADMIN_PROTECTED, ROLE_NAME_EXISTS -> 409;
            default -> 400;
        };
    }
}
