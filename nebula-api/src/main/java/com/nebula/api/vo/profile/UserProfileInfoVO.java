package com.nebula.api.vo.profile;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户基本信息
 */
@Data
@Accessors(chain=true)
public class UserProfileInfoVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 登录账号（NID，唯一）
     */
    private String nid;

    /**
     * 用户名
     */
    private String username;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 身份描述列表
     */
    private List<String> roles;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @Nonnull
    public static UserProfileInfoVO create() {
        return new UserProfileInfoVO();
    }
}
