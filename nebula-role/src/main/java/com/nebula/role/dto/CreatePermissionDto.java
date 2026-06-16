package com.nebula.role.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CreatePermissionDto {
    @NotBlank(message = "权限代码不能为空")
    private String name;
    private String description;
}
