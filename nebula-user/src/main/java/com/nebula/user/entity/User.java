package com.nebula.user.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
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

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String username;

    private String password;

    private String role = "USER";

    /**
     * 头像
     */
    private String avatar;

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
    @Column(comment = "状态 0正常 1禁用")
    private Integer status;

    @Column(isLogicDelete = true, comment = "逻辑删除 0未删除 1已删除")
    private Integer deleted;

    public static User create() {
        return new User();
    }
}