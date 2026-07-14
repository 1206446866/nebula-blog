package com.nebula.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoginType {

    PASSWORD(1, "账号密码"),
    EMAIL(2, "邮箱验证码"),
    GITHUB(3, "GitHub"),
    GOOGLE(4, "Google");

    private final Integer code;

    private final String desc;
}
