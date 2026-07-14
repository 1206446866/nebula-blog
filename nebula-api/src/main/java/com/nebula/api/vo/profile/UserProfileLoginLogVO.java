package com.nebula.api.vo.profile;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain=true)
public class UserProfileLoginLogVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录方式
     */
    private String loginType;

    /**
     * 登录设备
     */
    private String device;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 登录地区
     */
    private String location;

    /**
     * 登录状态（0：失败，1：成功）
     */
    private Integer status;

    /**
     * 登录时间
     */
    private LocalDateTime createTime;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    public static UserProfileLoginLogVO create(){
        return new UserProfileLoginLogVO();
    }
}
