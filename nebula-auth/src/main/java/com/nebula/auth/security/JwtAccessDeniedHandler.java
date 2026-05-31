package com.nebula.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 无权限处理
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException {

        response.setCharacterEncoding("UTF-8");

        response.setContentType("application/json;charset=UTF-8");

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        Result<Void> result = Result.error(403, "权限不足");

        String json = new ObjectMapper().writeValueAsString(result);

        response.getWriter().write(json);
    }
}