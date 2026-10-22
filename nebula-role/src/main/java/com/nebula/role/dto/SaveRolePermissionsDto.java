package com.nebula.role.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Data
@Accessors(chain = true)
public class SaveRolePermissionsDto {

    @NotNull(message = "请选择角色")
    @Min(value = 1, message = "角色ID非法")
    private Long roleId;
    @NotNull(message = "权限列表不能为空")
    @Size(max = 500, message = "单次最多分配500项权限")
    private List<@NotNull @Min(1) Long> permissionIds;
}
