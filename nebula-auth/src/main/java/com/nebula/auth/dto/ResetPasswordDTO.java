package com.nebula.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.nio.charset.StandardCharsets;

@Data
public class ResetPasswordDTO {
    @NotBlank(message = "请输入你的当前密码")
    private String operatorPassword;
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码需为8至64个字符")
    private String newPassword;
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @AssertTrue(message = "新密码编码长度不能超过72字节")
    public boolean isPasswordLengthValid() {
        return newPassword == null || newPassword.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
