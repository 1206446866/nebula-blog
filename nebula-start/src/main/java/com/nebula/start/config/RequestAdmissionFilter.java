package com.nebula.start.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.common.exception.ResourceLimitException;
import com.nebula.common.result.Result;
import com.nebula.common.security.RequestAdmissionGuard;
import com.nebula.common.security.RequestProtectionProperties;
import com.nebula.common.util.ClientIpResolver;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPatternParser;
import java.io.*;
import java.util.Locale;
import com.nebula.common.exception.DatabaseUnavailable;
import com.nebula.common.security.DatabaseReadBudget;

/** 只在Security链中注册，认证及JSON解析前先领取本地名额；上传继续使用独立的文件限制。 */
public class RequestAdmissionFilter extends OncePerRequestFilter {
    private static final org.springframework.web.util.pattern.PathPattern LOGIN = new PathPatternParser().parse("/auth/login");
    private final RequestAdmissionGuard guard;
    private final RequestProtectionProperties limits;
    private final ClientIpResolver ips;
    private final ObjectMapper json;

    public RequestAdmissionFilter(RequestAdmissionGuard guard, RequestProtectionProperties limits, ClientIpResolver ips, ObjectMapper json) {
        this.guard = guard; this.limits = limits; this.ips = ips; this.json = json;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        com.nebula.common.security.ResourceGuard.Permit permit;
        try { permit = guard.enter(ips.getIp(request)); }
        catch (ResourceLimitException failure) {
            reject(response, failure.getHttpStatus(), failure.getMessage(), failure.getRetryAfterSeconds()); return;
        }
        try (permit) {
            String type = request.getContentType();
            String media = type == null ? "" : type.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
            if (media.equals("application/json") || media.startsWith("application/") && media.endsWith("+json")) {
                var path = PathContainer.parsePath(request.getRequestURI().substring(request.getContextPath().length()));
                int maximum = LOGIN.matches(path) ? Math.min(limits.getLoginBytes(), limits.getJsonBytes()) : limits.getJsonBytes();
                if (request.getContentLengthLong() > maximum) { reject(response, 413, "请求正文过大", null); return; }
                // 实际读取同样有界，不能依靠缺失或不可信的Content-Length绕过限制。
                byte[] bytes;
                try { bytes = readJson(request, maximum); }
                catch (java.net.SocketTimeoutException timeout) {
                    response.setHeader("Connection", "close");
                    reject(response, 408, "请求正文读取超时，请重新发送", null); return;
                } catch (IOException failure) {
                    // Tomcat会包装套接字超时；真正断连继续交由容器收尾，名额由try自动释放。
                    if (!hasSocketTimeout(failure)) throw failure;
                    response.setHeader("Connection", "close");
                    reject(response, 408, "请求正文读取超时，请重新发送", null); return;
                }
                if (bytes.length > maximum) { reject(response, 413, "请求正文过大", null); return; }
                request = new BufferedJsonRequest(request, bytes);
            }
            try (var budget = isRead(request) ? DatabaseReadBudget.open(limits.getReadTimeoutSeconds()) : null) {
                chain.doFilter(request, response);
            } catch (ServletException | RuntimeException failure) {
                if (!DatabaseUnavailable.matches(failure) || response.isCommitted()) throw failure;
                reject(response, 503, "数据库繁忙，请稍后重试", 2L);
            }
        }
    }

    /** 分段检查累计期限；容器的5秒空闲读取限制负责终止没有新字节的阻塞。 */
    private byte[] readJson(HttpServletRequest request, int maximum) throws IOException {
        long deadline = System.nanoTime() + limits.getJsonReadTimeoutMs() * 1_000_000L;
        var result = new ByteArrayOutputStream(Math.min(maximum, 8192));
        var input = request.getInputStream();
        byte[] buffer = new byte[Math.min(maximum + 1, 8192)];
        while (result.size() <= maximum) {
            if (System.nanoTime() >= deadline) throw new java.net.SocketTimeoutException("JSON读取期限已耗尽");
            int count = input.read(buffer, 0, Math.min(buffer.length, maximum + 1 - result.size()));
            if (System.nanoTime() >= deadline) throw new java.net.SocketTimeoutException("JSON读取期限已耗尽");
            if (count < 0) break;
            result.write(buffer, 0, count);
        }
        return result.toByteArray();
    }

    /** HEAD与GET使用相同读取预算。 */
    private boolean isRead(HttpServletRequest request) {
        return "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
    }

    /** 超时原因链有界遍历，避免容器包装掩盖读取超时。 */
    private boolean hasSocketTimeout(Throwable failure) {
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable, Boolean>());
        while (failure != null && seen.add(failure)) {
            if (failure instanceof java.net.SocketTimeoutException) return true;
            failure = failure.getCause();
        }
        return false;
    }

    /** 解析前的拒绝统一返回公开错误，不回显正文或认证信息。 */
    private void reject(HttpServletResponse response, int status, String message, Long retry) throws IOException {
        response.setStatus(status); response.setHeader("Cache-Control", "no-store");
        if (status == 413) response.setHeader("Connection", "close");
        if (retry != null) response.setHeader("Retry-After", retry.toString());
        response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getOutputStream(), Result.error(status, message));
    }

    /** 保存已限长的JSON字节供同步MVC读取，不缓存Multipart，也不改动原始内容和编码。 */
    private static final class BufferedJsonRequest extends HttpServletRequestWrapper {
        private final byte[] bytes;
        private BufferedJsonRequest(HttpServletRequest request, byte[] bytes) { super(request); this.bytes = bytes; }
        @Override public int getContentLength() { return bytes.length; }
        @Override public long getContentLengthLong() { return bytes.length; }
        @Override public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding() == null ? "UTF-8" : getCharacterEncoding()));
        }
        @Override public ServletInputStream getInputStream() {
            var input = new ByteArrayInputStream(bytes);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public int read(byte[] target, int offset, int length) { return input.read(target, offset, length); }
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException("JSON接口使用同步读取"); }
            };
        }
    }
}
