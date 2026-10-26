package com.nebula.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordDTO {

    /**
     * 旧密码
     */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @com.nebula.common.validation.NewPassword
    private String newPassword;

    /**
     * 必填确认密码，必须与新密码原文一致
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
