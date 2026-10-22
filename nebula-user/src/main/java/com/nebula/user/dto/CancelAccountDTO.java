package com.nebula.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 当前登录用户注销自己的账号，不接受目标用户 ID。 */
@Data
public class CancelAccountDTO {
    @NotBlank(message = "请输入当前密码")
    private String password;

    @NotNull(message = "请确认注销账号")
    @AssertTrue(message = "请确认注销账号")
    private Boolean confirmed;
}
