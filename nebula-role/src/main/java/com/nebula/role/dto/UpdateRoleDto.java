package com.nebula.role.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRoleDto {
    @NotNull(message = "角色ID不能为空")
    private Long id;
    private String name;
    private String description;
}
