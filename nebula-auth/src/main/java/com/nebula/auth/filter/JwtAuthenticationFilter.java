package com.nebula.auth.filter;

import com.nebula.auth.security.AuthLoginUser;
import com.nebula.auth.security.TokenSessionUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.common.result.Result;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import com.nebula.auth.util.JwtUtil;
import com.nebula.common.constant.UserStatus;
import com.nebula.role.service.PermissionService;
import com.nebula.role.service.RoleService;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static com.nebula.user.entity.table.UserTableDef.USER;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @org.springframework.beans.factory.annotation.Value("${nebula.security.request.authentication-timeout-seconds:3}")
    private int authenticationTimeoutSeconds = 3;

    private final JwtUtil jwtUtil;

    private final UserMapper userMapper;

    private final RoleService roleService;

    private final PermissionService permissionService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String uri = request.getRequestURI();
        if (uri.startsWith("/upload")) {
            filterChain.doFilter(request, response);
            return;
        }
        // 没有Token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        // 提取Token
        String token = authHeader.substring(7);
        Claims claims;
        Long userId;
        try {
            claims = jwtUtil.parseToken(token);
            userId = Long.parseLong(claims.getSubject());
            if (userId <= 0) throw new IllegalArgumentException("Invalid subject");
        } catch (JwtException | IllegalArgumentException e) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthLoginUser authLoginUser;
        try {
            authLoginUser = authenticate(claims, userId);
        } catch (TokenSessionUnavailableException e) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setHeader("Retry-After", "2");
            response.setHeader("Cache-Control", "no-store");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new ObjectMapper().writeValueAsString(Result.error(503, e.getMessage())));
            return;
        }
        if (authLoginUser == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 创建认证对象
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(authLoginUser, null, authLoginUser.getAuthorities());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 存入Security上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    /** 用户、会话和权限核验共用认证期限；失败时不建立身份，期限不延续到写业务。 */
    private AuthLoginUser authenticate(Claims claims, Long userId) {
        try (var budget = com.nebula.common.security.DatabaseReadBudget.open(authenticationTimeoutSeconds)) {
            User user = userMapper.selectOneByCondition(USER.ID.eq(userId));
            if (user == null || !UserStatus.NORMAL.matches(user.getStatus())
                    || Integer.valueOf(1).equals(user.getDeleted())) return null;
            if (!jwtUtil.isSessionActive(claims, user)) return null;
            com.nebula.common.security.DatabaseReadBudget.remainingSeconds();
            List<String> roles = roleService.getRolesByUserId(userId).stream().map(role -> role.getName()).toList();
            List<String> permissions = permissionService.getPermissionsByUserId(userId);
            com.nebula.common.security.DatabaseReadBudget.remainingSeconds();
            return new AuthLoginUser(user, roles, permissions);
        }
    }

}
