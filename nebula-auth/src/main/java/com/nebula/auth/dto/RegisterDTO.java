package com.nebula.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RegisterDTO {

    @NotBlank(message = "注册链接不能为空")
    private String token;

}