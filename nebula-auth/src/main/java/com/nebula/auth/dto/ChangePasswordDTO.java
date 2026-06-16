package com.nebula.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangePasswordDTO {

    /**
     * 旧密码
     */
    @NotNull(message = "旧密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotNull(message = "新密码不能为空")
    private String newPassword;

    /**
     * 确认密码（可选，但推荐）
     */
    private String confirmPassword;
}