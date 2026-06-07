package com.nebula.common.constant;

import lombok.Getter;

/**
 * 系统角色枚举，对应 user_role 表
 */
@Getter
public enum RoleEnum {

    SUPER_ADMIN(1L, "SUPER_ADMIN"),
    ADMIN(2L, "ADMIN"),
    USER(3L, "USER");

    /**
     * 数据库中 role_id
     */
    private final Long roleId;

    /**
     * 数据库中 role_name / 标识
     */
    private final String code;

    RoleEnum(Long id, String code) {
        this.roleId = id;
        this.code = code;
    }

    /**
     * 根据 role_id 获取枚举
     */
    public static RoleEnum fromId(Long id) {
        for (RoleEnum role : values()) {
            if (role.roleId.equals(id)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 根据 role_name 获取枚举
     */
    public static RoleEnum fromCode(String code) {
        for (RoleEnum role : values()) {
            if (role.code.equalsIgnoreCase(code)) {
                return role;
            }
        }
        return null;
    }
}