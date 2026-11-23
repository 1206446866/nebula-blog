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

    /**
     * 登录账号（NID，唯一）
     */
    private String nid;

    private String username;

    private String password;

    /** 账号会话版本，更新后此前签发的会话统一失效。 */
    @Column(onInsertValue = "0")
    private Long sessionVersion;

    /** 数据库默认版本为零，新建对象尚未回读时也使用该版本。 */
    public long currentSessionVersion() {
        return sessionVersion == null ? 0L : sessionVersion;
    }


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
    @Column(comment = "状态 0正常 1禁用 参考UserStatus")
    private Integer status;

    @Column(isLogicDelete = true, comment = "逻辑删除 0未删除 1已删除")
    private Integer deleted;

    private String email;

    private String phone;

    public static User create() {
        return new User();
    }
}
