package com.nebula.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class EditUserDTO {
    @NotNull(message = "用户ID不能为空")
    private Long id;
    private String username;
    @NotEmpty(message = "用户至少需要一个角色")
    @Size(max = 32, message = "单个用户最多分配32个角色")
    private List<@NotNull @Min(1) Long> roleIds;
}
