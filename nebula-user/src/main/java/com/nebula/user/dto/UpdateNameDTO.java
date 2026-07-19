package com.nebula.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateNameDTO {

    @NotBlank(message = "用户名不能为空")
    @Max(value = 20,message = "用户昵称长度不能超过20")
    @Min(value = 3, message = "用户昵称长度不能少于3")
    private String username;
}
