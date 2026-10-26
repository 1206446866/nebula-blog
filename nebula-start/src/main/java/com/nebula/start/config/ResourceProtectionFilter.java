package com.nebula.start.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.common.exception.ResourceLimitException;
import com.nebula.common.result.Result;
import com.nebula.common.security.ResourceGuard;
import com.nebula.common.util.ClientIpResolver;
import com.nebula.common.util.SecurityUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import java.io.IOException;

/** 认证后、Multipart解析前限制高成本入口；只在Security过滤链注册一次。 */
public class ResourceProtectionFilter extends OncePerRequestFilter {
    private static final PathPattern ARTICLE_UPLOAD = pattern("/media/images");
    private static final PathPattern AVATAR_UPLOAD = pattern("/users/upload/avatar");
    private static final PathPattern SEARCH = pattern("/search");
    private static final PathPattern MEDIA_LIST = pattern("/media");
    private static final PathPattern MEDIA_FILE = pattern("/media/{*path}");
    private static final PathPattern PUBLIC_FILE = pattern("/upload/{*path}");
    private final ResourceGuard guard;
    private final ClientIpResolver ips;
    private final ObjectMapper json;
    public ResourceProtectionFilter(ResourceGuard guard, ClientIpResolver ips, ObjectMapper json) {
        this.guard = guard; this.ips = ips; this.json = json;
    }
    @Override protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        // 与Spring MVC使用相同的路径分段和解码规则，编码字符不能绕过预算。
        PathContainer path = PathContainer.parsePath(request.getRequestURI().substring(request.getContextPath().length()));
        var work = work(request.getMethod(), path);
        Long user = SecurityUtils.getLoginUser() == null ? null : SecurityUtils.getUserId();
        if (work == null || user == null && !PUBLIC_FILE.matches(path)) {
            chain.doFilter(request, response); return;
        }
        ResourceGuard.Permit permit;
        try { permit = guard.enter(work, ips.getIp(request), user); }
        catch (ResourceLimitException e) {
            response.setStatus(e.getHttpStatus());
            response.setHeader("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
            response.setHeader("Cache-Control", "no-store");
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            json.writeValue(response.getOutputStream(), Result.error(e.getHttpStatus(), e.getMessage()));
            return;
        }
        try (permit) { chain.doFilter(request, response); }
    }
    /** 对实际路由分类，匿名私有接口仍由后续认证链返回401。 */
    private ResourceGuard.Work work(String method, PathContainer path) {
        if (method.equals("POST") && (ARTICLE_UPLOAD.matches(path) || AVATAR_UPLOAD.matches(path))) return ResourceGuard.Work.UPLOAD;
        if (method.equals("DELETE") && MEDIA_FILE.matches(path)) return ResourceGuard.Work.READ;
        if (method.equals("GET") || method.equals("HEAD")) {
            if (SEARCH.matches(path)) return ResourceGuard.Work.SEARCH;
            if (MEDIA_LIST.matches(path) || MEDIA_FILE.matches(path) || PUBLIC_FILE.matches(path)) return ResourceGuard.Work.READ;
        }
        // 评论、点赞、草稿、关注及管理操作共用写入预算，换入口不会重置额度。
        if (java.util.Set.of("POST", "PUT", "PATCH", "DELETE").contains(method)) return ResourceGuard.Work.WRITE;
        return null;
    }
    /** 预编译应用相对路径，保持与默认MVC路径匹配行为一致。 */
    private static PathPattern pattern(String path) {
        return new PathPatternParser().parse(path);
    }
}
