package com.nebula.role.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UpdatePermissionDto {
    @NotNull(message = "权限id不能为空")
    @Min(value = 1,message = "非法的权限ID")
    private Long id;
    @NotBlank(message = "权限代码不能为空")
    private String name;
    private String description;
}
