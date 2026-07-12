package com.nebula.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EditUserDTO {
    @NotNull(message = "用户ID不能为空")
    private Long id;
    private String username;
    private List<Long> roleIds;
}