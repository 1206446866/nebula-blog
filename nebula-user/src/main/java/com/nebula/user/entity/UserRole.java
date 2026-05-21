package com.nebula.user.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Table("user_role")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class UserRole extends Model<UserRole> {

    private Long userId;

    private Long roleId;

    /**
     * 创建时间
     */
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;
}