package com.nebula.role.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data(staticConstructor = "create")
@Table("role_permission")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class RolePermission extends Model<RolePermission> {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long roleId;
    private Long permissionId;
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;
}