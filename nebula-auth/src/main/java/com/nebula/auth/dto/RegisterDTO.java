package com.nebula.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RegisterDTO {

    @NotBlank(message = "注册链接不能为空")
    @Pattern(regexp = "[a-f0-9]{32}", message = "注册链接无效")
    private String token;

}
