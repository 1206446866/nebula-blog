package com.nebula.auth.dto;

import com.nebula.common.constant.RegisterType;
import lombok.Data;

@Data
public class RegisterCacheDTO {

    /**
     * 注册账号
     * 邮箱/手机号
     */
    private String account;

    /**
     * BCrypt加密后的密码
     */
    private String password;

    /**
     * 注册类型
     */
    private RegisterType type;
}