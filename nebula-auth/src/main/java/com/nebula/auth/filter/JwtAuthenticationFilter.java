package com.nebula.auth.filter;

import com.nebula.auth.security.LoginUser;
import com.nebula.auth.service.PermissionService;
import com.nebula.auth.util.JwtUtil;
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

    private final PermissionService permissionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) throws ServletException, IOException {
        System.out.println("JWT FILTER EXECUTED");
        String authHeader = request.getHeader("Authorization");
        System.out.println("authHeader: " + authHeader);
        // 没有Token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);

            return;
        }
        // 提取Token
        String token = authHeader.substring(7);

        // 校验Token
        if (!jwtUtil.validateToken(token)) {

            filterChain.doFilter(request, response);

            return;
        }

        // 获取用户ID
        Long userId = jwtUtil.getUserId(token);

        // 查询用户
        User user = userMapper.selectOneByCondition(USER.ID.eq(userId));

        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 查询用户权限
        List<String> permissions = permissionService.getPermissionsByUserId(userId);
        // 构建登录用户
        LoginUser loginUser = new LoginUser(user,permissions);
        System.out.println("----------------------------------");
        System.out.println(loginUser.getAuthorities());
        // 创建认证对象
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 存入Security上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }


}