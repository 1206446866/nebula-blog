package com.nebula.user.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Table("user")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class User extends Model<User> {

    private Long id;

    private String username;

    private String password;

    private String role = "USER";

    /**
     * 创建时间
     */
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;

    /**
     * 状态
     */
    @Column(isLogicDelete = true)
    private Integer status;
}