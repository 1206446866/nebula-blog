package com.nebula.role.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data(staticConstructor = "create")
@Table("permission")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class Permission extends Model<Permission> {

    private Long id;

    private String name; // 权限名称，例如 "user:create"

    private String description; // 描述

    @Column(onInsertValue = "now()")
    private java.time.LocalDateTime createTime;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private java.time.LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer status; // 0: 删除, 1: 正常

    private Long parentId;
}