package com.nebula.auth.service.impl;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nebula.auth.entity.LoginLog;
import com.nebula.auth.mapper.LoginLogMapper;
import com.nebula.auth.service.LocationService;
import com.nebula.auth.service.LoginLogService;
import com.nebula.common.constant.LoginType;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.util.RequestUtils;
import com.nebula.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.nebula.auth.entity.table.LoginLogTableDef.LOGIN_LOG;
import static com.nebula.common.exception.code.AuthErrorCode.USER_NOT_FOUND;
import static com.nebula.user.entity.table.UserTableDef.USER;

@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {

    private final HttpServletRequest request;

    private final LocationService locationService;

    @Override
    public void recordLoginLog(Long userId, Integer status) {
        String ip = RequestUtils.getIp(request);
        String userAgent = request.getHeader("User-Agent");
        UserAgent ua = UserAgentUtil.parse(userAgent);
        String browser = ua.getBrowser().getName();
        String os = ua.getOs().getName();
        String device;
        String location = locationService.getLocation(ip);
        if (ua.isMobile()) {
            device = "Mobile";
        } else {
            device = "PC";
        }
        LoginLog.create().setUserId(userId)
                .setLoginType(LoginType.PASSWORD.getDesc())
                .setStatus(status)
                .setIp(ip)
                .setUserAgent(userAgent)
                .setBrowser(browser)
                .setOs(os)
                .setDevice(device)
                .setLocation(location).save();
    }

    @Override
    public List<LoginLog> getLoginLog(Long userId) {
        User user = User.create().where(USER.ID.eq(userId));
        if (user == null) {throw new BusinessException(USER_NOT_FOUND);}
        QueryWrapper queryWrapper = QueryWrapper.create().where(LOGIN_LOG.USER_ID.eq(userId)).orderBy(LOGIN_LOG.CREATE_TIME,false).limit(20);
        return list(queryWrapper);
    }
}
