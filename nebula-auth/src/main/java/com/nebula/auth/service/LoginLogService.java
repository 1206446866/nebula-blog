package com.nebula.auth.service;

import com.mybatisflex.core.service.IService;
import com.nebula.auth.entity.LoginLog;

import java.util.List;

/** 用户登录记录服务。 */
public interface LoginLogService extends IService<LoginLog> {

    /**
     * 记录已存在用户的登录结果及请求来源，兼容缺失的浏览器信息。
     *
     * @param userId 用户ID
     * @param status 登录结果码，取自LoginStatus
     */
    void recordLoginLog( Long userId, Integer status );

    /**
     * 按时间倒序查询用户最近20条登录记录。
     *
     * @param userId 用户ID
     * @return 最近登录记录列表
     */
    List<LoginLog> getLoginLog(Long userId);
}
