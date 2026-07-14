package com.nebula.auth.service;

import com.mybatisflex.core.service.IService;
import com.nebula.auth.entity.LoginLog;

import java.util.List;

public interface LoginLogService extends IService<LoginLog> {

    void recordLoginLog( Long userId, Integer status );

    List<LoginLog> getLoginLog(Long userId);
}
