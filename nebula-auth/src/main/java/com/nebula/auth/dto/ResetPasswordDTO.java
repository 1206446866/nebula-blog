package com.nebula.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class ResetPasswordDTO {
    @NotBlank(message = "请输入你的当前密码")
    private String operatorPassword;
    @NotBlank(message = "新密码不能为空")
    @com.nebula.common.validation.NewPassword
    private String newPassword;
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
