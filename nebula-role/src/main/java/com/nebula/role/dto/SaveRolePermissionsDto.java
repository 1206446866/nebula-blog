package com.nebula.role.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class SaveRolePermissionsDto {

    private Long roleId;
    private List<Long> permissionIds;
}
