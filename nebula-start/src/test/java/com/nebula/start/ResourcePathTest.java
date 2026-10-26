package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.common.exception.ResourceLimitException;
import com.nebula.common.security.*;
import com.nebula.common.util.ClientIpResolver;
import com.nebula.start.config.ResourceProtectionFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** 路由编码、上下文路径和多种业务写入均必须进入相应预算。 */
class ResourcePathTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @ParameterizedTest
    @CsvSource({"GET,/search,/search,SEARCH", "GET,/se%61rch,/search,SEARCH", "POST,/m%65dia/images,/media/images,UPLOAD",
            "POST,/users/upl%6fad/avatar,/users/upload/avatar,UPLOAD", "GET,/u%70load/article/fixture,/upload/article/fixture,READ",
            "PUT,/comments/release,/comments/release,WRITE", "POST,/articles/drafts,/articles/drafts,WRITE",
            "PUT,/articles/like,/articles/like,WRITE", "DELETE,/community/following/2,/community/following/2,WRITE"})
    void encodedAndWritePathsCannotSkipTheGuard(String method, String uri, String decoded, ResourceGuard.Work work) throws Exception {
        LoginUser user = mock(LoginUser.class); when(user.getUserId()).thenReturn(123L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));
        ResourceGuard guard = mock(ResourceGuard.class);
        when(guard.enter(eq(work),anyString(),eq(123L))).thenThrow(new ResourceLimitException(429,60,"验证用超限"));
        var filter = new ResourceProtectionFilter(guard,new ClientIpResolver(List.of()),new ObjectMapper());
        var request = new MockHttpServletRequest(method,"/context"+uri);
        request.setContextPath("/context"); request.setServletPath(decoded); request.setRemoteAddr("127.0.0.1");
        var response = new MockHttpServletResponse();
        filter.doFilter(new StrictHttpFirewall().getFirewalledRequest(request),response,(req,res)->fail("超限后不能进入业务"));
        assertEquals(429,response.getStatus()); verify(guard).enter(work,"127.0.0.1",123L);
    }
}
