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

        // 查询用户
        User user = userMapper.selectOneByCondition(USER.ID.eq(userId));

        if (user == null || !UserStatus.NORMAL.matches(user.getStatus())
                || Integer.valueOf(1).equals(user.getDeleted())) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean active;
        try {
            active = jwtUtil.isSessionActive(claims, user);
        } catch (TokenSessionUnavailableException e) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new ObjectMapper().writeValueAsString(Result.error(503, e.getMessage())));
            return;
        }
        if (!active) {
            filterChain.doFilter(request, response);
            return;
        }

        // 查询用户权限
        List<String> roles = roleService.getRolesByUserId(userId).stream().map(role -> role.getName()).toList();
         // 查询用户权限
        List<String> permissions = permissionService.getPermissionsByUserId(userId);
        // 构建登录用户
        AuthLoginUser authLoginUser = new AuthLoginUser(user,roles, permissions);
        // 创建认证对象
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(authLoginUser, null, authLoginUser.getAuthorities());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 存入Security上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }


}
