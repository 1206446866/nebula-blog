package com.nebula.auth.entity;

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
@Table("login_log")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class LoginLog extends Model<LoginLog> {

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
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
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * User-Agent
     */
    private String userAgent;

    public static LoginLog create(){
        return new LoginLog();
    }
}
