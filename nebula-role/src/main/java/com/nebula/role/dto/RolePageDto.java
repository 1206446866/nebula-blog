package com.nebula.role.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class RolePageDto {

    private String description;

    @Min(1)
    private int current = 1;

    @Min(1)
    @Max(100)
    private int size = 10;
}
