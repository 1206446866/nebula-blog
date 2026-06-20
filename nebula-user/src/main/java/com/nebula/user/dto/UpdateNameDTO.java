package com.nebula.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateNameDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;
}
