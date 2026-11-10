package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.common.security.*;
import com.nebula.common.util.ClientIpResolver;
import com.nebula.start.config.RequestAdmissionFilter;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

/** 从过滤器边界检查实际字节限制、分块读取、Multipart隔离及失败释放。 */
class RequestAdmissionFilterTest {
    private final RequestProtectionProperties limits = new RequestProtectionProperties();
    private final RequestAdmissionGuard guard = new RequestAdmissionGuard(limits);
    private final RequestAdmissionFilter filter = new RequestAdmissionFilter(guard, limits, new ClientIpResolver(List.of()), new ObjectMapper());

    @Test void declaredOversizedJsonIsRejectedBeforeOpeningTheStream() throws Exception {
        limits.setJsonBytes(256);
        var request = new MockHttpServletRequest("POST", "/articles/create") {
            @Override public jakarta.servlet.ServletInputStream getInputStream() { fail("长度已超限时不读取正文"); return null; }
        };
        request.setContentType("application/json"); request.setContent(new byte[257]);
        var response = new MockHttpServletResponse();
        filter.doFilter(request,response,(a,b)->fail("不应进入JWT或MVC"));
        assertEquals(413,response.getStatus()); assertEquals("no-store",response.getHeader("Cache-Control"));
    }

    @Test void missingOrFalseLengthCannotBypassTheActualByteLimit() throws Exception {
        limits.setJsonBytes(256);
        for (long reported : new long[]{-1, 1}) {
            var request = new MockHttpServletRequest("POST", "/articles/create") {
                @Override public long getContentLengthLong() { return reported; }
            };
            request.setContentType("application/problem+json"); request.setContent(new byte[257]);
            var response = new MockHttpServletResponse();
            filter.doFilter(request,response,(a,b)->fail("未声明真实长度仍应拒绝"));
            assertEquals(413,response.getStatus());
        }
    }

    @Test void exactLimitAndUtf8BytesArePreserved() throws Exception {
        byte[] original = "{\"text\":\"星云🌌\"}".getBytes(StandardCharsets.UTF_8);
        limits.setJsonBytes(original.length);
        var request = json("/articles/create", original);
        filter.doFilter(request,new MockHttpServletResponse(),(a,b)->assertArrayEquals(original,a.getInputStream().readAllBytes()));
        limits.setJsonBytes(original.length-1);
        var response = new MockHttpServletResponse();
        filter.doFilter(json("/articles/create", original),response,(a,b)->fail("按字节而非字符限制"));
        assertEquals(413,response.getStatus());
    }

    @Test void encodedLoginPathUsesSmallerBudgetWhileWritingRetainsItsOwnBudget() throws Exception {
        limits.setLoginBytes(256); limits.setJsonBytes(1024);
        var response = new MockHttpServletResponse();
        filter.doFilter(json("/auth/%6cogin",new byte[257]),response,(a,b)->fail("编码不能切换预算"));
        assertEquals(413,response.getStatus());
        var passed = new AtomicBoolean();
        filter.doFilter(json("/articles/create",new byte[257]),new MockHttpServletResponse(),(a,b)->passed.set(true));
        assertTrue(passed.get());
    }

    @Test void multipartIsNotReadOrBufferedBeforeItsExistingUploadProtection() throws Exception {
        var request = new MockHttpServletRequest("POST", "/media/images") {
            @Override public jakarta.servlet.ServletInputStream getInputStream() { fail("不能提前解析上传"); return null; }
        };
        request.setContentType("multipart/form-data; boundary=probe");
        filter.doFilter(request,new MockHttpServletResponse(),(a,b)->assertSame(request,a));
    }

    @Test void ioAndDownstreamFailuresAlwaysReturnAdmissionSlots() throws Exception {
        limits.setIpConcurrency(1); limits.setMaxConcurrency(1);
        var broken = new MockHttpServletRequest("POST", "/articles/create") {
            @Override public jakarta.servlet.ServletInputStream getInputStream() { throw new java.io.UncheckedIOException(new IOException("读取失败")); }
        };
        broken.setContentType("application/json");
        assertThrows(java.io.UncheckedIOException.class,()->filter.doFilter(broken,new MockHttpServletResponse(),(a,b)->fail()));
        var first = new MockHttpServletRequest("GET", "/articles");
        assertThrows(ServletException.class,()->filter.doFilter(first,new MockHttpServletResponse(),(a,b)->{throw new ServletException("下游失败");}));
        var passed = new AtomicBoolean();
        filter.doFilter(new MockHttpServletRequest("GET", "/articles"),new MockHttpServletResponse(),(a,b)->passed.set(true));
        assertTrue(passed.get());
    }

    private MockHttpServletRequest json(String path, byte[] content) {
        var request = new MockHttpServletRequest("POST",path); request.setContentType("application/json;charset=UTF-8"); request.setContent(content); return request;
    }
}
