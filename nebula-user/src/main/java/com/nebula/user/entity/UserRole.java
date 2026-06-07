package com.nebula.user.entity;

import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Table("user_role")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class UserRole extends Model<UserRole> {

    private Long userId;

    private Long roleId;

    public static UserRole create() {
        return new UserRole();
    }

}