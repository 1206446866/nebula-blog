package com.nebula.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@Accessors(chain=true)
public class LoginDTO {

    /**
     * 账号
     */
    @NotBlank(message = "NID/邮箱不能为空")
    @Size(max = 128, message = "账号不能超过128个字符")
    private String account;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(max = 1024, message = "登录密码过长")
    private String password;
}
