package com.nebula.role.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class PermissionVO {

    private Long id;

    private String name;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
