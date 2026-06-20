package com.nebula.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum UserStatus {

    NORMAL(NumberConstant.ZERO, "正常"),

    DISABLED(NumberConstant.ONE, "禁用"),

    LOCKED(NumberConstant.TWO, "封禁");

    private final Integer code;
    private final String description;


    public boolean matches(Integer code) {
        return this.code.equals(code);
    }

    public boolean notMatches(Integer code) {
        return this.code.equals(code);
    }

    //code反向查找枚举
    public static UserStatus of(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知状态：" + code));
    }
}
