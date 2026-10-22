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
import com.nebula.common.util.ClientIpResolver;
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

    private static final String UNKNOWN = "Unknown";

    private final HttpServletRequest request;

    private final LocationService locationService;
    private final ClientIpResolver clientIps;

    @Override
    public void recordLoginLog(Long userId, Integer status) {
        String ip = clientIps.getIp(request);
        String userAgent = logValue(request.getHeader("User-Agent"), 512);
        UserAgent ua = parseUserAgent(userAgent);
        String browser = ua == null || ua.getBrowser() == null ? UNKNOWN : logValue(ua.getBrowser().getName(), 50);
        String os = ua == null || ua.getOs() == null ? UNKNOWN : logValue(ua.getOs().getName(), 50);
        String device = UNKNOWN;
        String location = locationService.getLocation(ip);
        if (ua != null && ua.isMobile()) {
            device = "Mobile";
        } else if (!UNKNOWN.equalsIgnoreCase(browser) || !UNKNOWN.equalsIgnoreCase(os)) {
            device = "PC";
        }
        save(LoginLog.create().setUserId(userId)
                .setLoginType(LoginType.PASSWORD.getDesc())
                .setStatus(status)
                .setIp(ip)
                .setUserAgent(userAgent)
                .setBrowser(browser)
                .setOs(os)
                .setDevice(device)
                .setLocation(location));
    }

    private static UserAgent parseUserAgent(String userAgent) {
        if (UNKNOWN.equals(userAgent)) return null;
        try {
            return UserAgentUtil.parse(userAgent);
        } catch (RuntimeException ignored) {
            // Client-supplied metadata must not change the authentication result.
            return null;
        }
    }

    private static String logValue(String value, int maxLength) {
        if (value == null) return UNKNOWN;
        String sanitized = value.codePoints().limit(maxLength)
                .collect(StringBuilder::new,
                        (text, codePoint) -> text.appendCodePoint(Character.isISOControl(codePoint) ? ' ' : codePoint),
                        StringBuilder::append)
                .toString().strip();
        return sanitized.isEmpty() ? UNKNOWN : sanitized;
    }

    @Override
    public List<LoginLog> getLoginLog(Long userId) {
        User user = User.create().where(USER.ID.eq(userId));
        if (user == null) {throw new BusinessException(USER_NOT_FOUND);}
        QueryWrapper queryWrapper = QueryWrapper.create().where(LOGIN_LOG.USER_ID.eq(userId)).orderBy(LOGIN_LOG.CREATE_TIME,false).limit(20);
        return list(queryWrapper);
    }
}
