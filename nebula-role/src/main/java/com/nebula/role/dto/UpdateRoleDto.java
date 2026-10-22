package com.nebula.role.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateRoleDto {
    @NotNull(message = "角色ID不能为空")
    @Min(1)
    private Long id;
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码最多50个字符")
    private String name;
    @Size(max = 255, message = "角色描述最多255个字符")
    private String description;
}
