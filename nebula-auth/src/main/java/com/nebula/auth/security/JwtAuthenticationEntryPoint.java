package com.nebula.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未登录处理
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {

        response.setCharacterEncoding("UTF-8");

        response.setContentType("application/json;charset=UTF-8");

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Result<Void> result = Result.error(401, "未登录或Token已过期");

        String json = new ObjectMapper().writeValueAsString(result);

        response.getWriter().write(json);
    }
}