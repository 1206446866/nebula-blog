package com.nebula.role.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class CreateRoleDto {
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码最多50个字符")
    private String name;
    @Size(max = 255, message = "角色描述最多255个字符")
    private String description;
}
