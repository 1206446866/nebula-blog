package com.nebula.auth.dto;

import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = "NID不能为空")
    private String nid;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
