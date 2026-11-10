package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.auth.controller.AuthController;
import com.nebula.auth.filter.JwtAuthenticationFilter;
import com.nebula.auth.provider.*;
import com.nebula.auth.security.*;
import com.nebula.auth.service.*;
import com.nebula.auth.util.JwtUtil;
import com.nebula.article.controller.ArticleController;
import com.nebula.article.service.ArticleService;
import com.nebula.api.controller.ApiController;
import com.nebula.api.facade.*;
import com.nebula.common.config.CorsConfig;
import com.nebula.common.exception.*;
import com.nebula.common.security.*;
import com.nebula.common.util.ClientIpResolver;
import com.nebula.role.service.*;
import com.nebula.start.config.SecurityConfig;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 实际Security与MVC链配合真实JWT，替身隔离数据库、Redis和业务写入，不启动服务器。 */
class RequestProtectionSecurityTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private UserMapper users;
    private ResourceGuard resources;
    private AuthService auth;
    private JwtUtil jwt;
    private TokenSessionService sessions;
    private RequestProtectionProperties limits;
    private User user;
    private String token;

    @BeforeEach void setup() {
        context = new AnnotationConfigWebApplicationContext(); context.setServletContext(new MockServletContext());
        context.register(Config.class); context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain",Filter.class)).build();
        users=context.getBean(UserMapper.class); resources=context.getBean(ResourceGuard.class); auth=context.getBean(AuthService.class);
        jwt=context.getBean(JwtUtil.class); sessions=context.getBean(TokenSessionService.class); limits=context.getBean(RequestProtectionProperties.class);
        user=User.create().setId(42L).setNid("42").setUsername("防护回归").setPassword("test-only-hash").setStatus(0).setDeleted(0);
        when(users.selectOneByCondition(any())).thenReturn(user);
        when(sessions.isActive(anyString(),any())).thenReturn(true);
        when(context.getBean(RoleService.class).getRolesByUserId(42L)).thenReturn(List.of());
        when(context.getBean(PermissionService.class).getPermissionsByUserId(42L)).thenReturn(List.of("article:list","article:create"));
        when(resources.enter(any(),anyString(),any())).thenAnswer(call -> (ResourceGuard.Permit) () -> {});
        token=jwt.createToken(user);
    }

    @AfterEach void cleanup() { context.close(); org.springframework.security.core.context.SecurityContextHolder.clearContext(); }

    @Test void admissionRejectsBeforeAnyAuthenticationDependencyDespiteRouteAndForwardedHeaderChanges() throws Exception {
        limits.setIpRequests(1);
        mvc.perform(get("/not-a-route").header("Authorization","Bearer "+token)).andExpect(status().isNotFound());
        clearInvocations(users,sessions,context.getBean(RoleService.class),context.getBean(PermissionService.class));
        mvc.perform(get("/articles").header("Authorization","Bearer "+token).header("X-Forwarded-For","192.0.2.200"))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After")).andExpect(header().string("Cache-Control","no-store"));
        verifyNoInteractions(users,sessions,context.getBean(RoleService.class),context.getBean(PermissionService.class));
    }

    @Test void ordinaryReadsShareOneBusinessBudgetAndHeadIsCovered() throws Exception {
        var count = new AtomicInteger();
        when(resources.enter(eq(ResourceGuard.Work.READ),anyString(),eq(42L))).thenAnswer(call -> {
            if (count.incrementAndGet()>2) throw new ResourceLimitException(429,3,"读取繁忙");
            return (ResourceGuard.Permit) () -> {};
        });
        mvc.perform(get("/articles").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(get("/api/profile/42").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(head("/articles/1").header("Authorization","Bearer "+token)).andExpect(status().isTooManyRequests());
        assertEquals(3,count.get());
    }

    @Test void giantJsonIsRejectedBeforeJwtOrLoginService() throws Exception {
        mvc.perform(post("/auth/login").contentType("application/json").content(" ".repeat(limits.getLoginBytes()+1))
                        .header("Authorization","Bearer "+token).with(request -> {request.removeHeader("Content-Length"); return request;}))
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.code").value(413));
        verifyNoInteractions(users,auth,context.getBean(AuthAbuseProtection.class));
    }

    @Test void loginUpperBoundsRejectHugeFieldsButHistoricalShortPasswordsReachAuthentication() throws Exception {
        mvc.perform(post("/auth/login").contentType("application/json").content("{\"account\":\""+"a".repeat(129)+"\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/auth/login").contentType("application/json").content("{\"account\":\"42\",\"password\":\""+"x".repeat(1025)+"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(auth);
        mvc.perform(post("/auth/login").contentType("application/json").content("{\"account\":\"42\",\"password\":\"1\"}"))
                .andExpect(status().isOk());
        verify(auth).login(argThat(dto -> dto.getAccount().equals("42") && dto.getPassword().equals("1")));
    }

    @Test void maximumEditorContentAndUnicodeSurviveTheFullChain() throws Exception {
        String content="星".repeat(199998)+"🌌";
        String body=new ObjectMapper().writeValueAsString(Map.of("title","长正文回归","content",content,"categoryId",1,"tagIds",List.of()));
        mvc.perform(post("/articles/create").contentType("application/json;charset=UTF-8").content(body).header("Authorization","Bearer "+token))
                .andExpect(status().isOk());
        verify(context.getBean(ArticleService.class)).createArticle(argThat(dto -> dto.getContent().equals(content)));
    }

    @Test void simultaneousNormalPageRequestsFitDefaultLimitsAndAFullSlotIsReleased() throws Exception {
        int parallel=8;
        limits.setIpConcurrency(parallel);
        var redis=mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class),anyList(),any(Object[].class))).thenReturn(0L);
        var actualResources=new ResourceGuard(redis,new ResourceLimitProperties());
        when(resources.enter(any(),anyString(),any())).thenAnswer(call -> actualResources.enter(call.getArgument(0),call.getArgument(1),call.getArgument(2)));
        var entered=new CountDownLatch(parallel); var release=new CountDownLatch(1);
        when(context.getBean(ArticleService.class).pageArticles(any(),any(),anyBoolean())).thenAnswer(call -> {
            entered.countDown(); assertTrue(release.await(10,TimeUnit.SECONDS)); return null;
        });
        var pool=Executors.newFixedThreadPool(parallel);
        var futures=new ArrayList<Future<Integer>>();
        try {
            for(int i=0;i<parallel;i++) futures.add(pool.submit(() -> mvc.perform(get("/articles").header("Authorization","Bearer "+token)).andReturn().getResponse().getStatus()));
            assertTrue(entered.await(10,TimeUnit.SECONDS),"正常页面的并行读取应能通过两层预算");
            mvc.perform(get("/articles").header("Authorization","Bearer "+token)).andExpect(status().isTooManyRequests());
            verify(users,times(parallel)).selectOneByCondition(any());
            release.countDown();
            for(var result:futures) assertEquals(200,result.get(10,TimeUnit.SECONDS));
        } finally { release.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS)); }
        mvc.perform(get("/articles").header("Authorization","Bearer "+token)).andExpect(status().isOk());
    }

    @Test void revokedSessionsAndMissingTokensRemainUnauthorizedAndCorsPreflightStillWorks() throws Exception {
        mvc.perform(get("/articles")).andExpect(status().isUnauthorized());
        when(sessions.isActive(anyString(),any())).thenReturn(false);
        mvc.perform(get("/articles").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        mvc.perform(options("/articles").header("Origin","http://localhost:5173").header("Access-Control-Request-Method","GET"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"));
    }

    @Test void databaseFailureDuringAuthenticationIsRetryableAndDoesNotLeaveAnIdentity() throws Exception {
        when(users.selectOneByCondition(any())).thenThrow(new org.springframework.jdbc.CannotGetJdbcConnectionException("内部连接地址"));
        mvc.perform(get("/articles").header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable()).andExpect(header().exists("Retry-After"))
                .andExpect(header().string("Cache-Control", "no-store"));
        verifyNoInteractions(context.getBean(ArticleService.class));
        doReturn(user).when(users).selectOneByCondition(any());
        when(context.getBean(PermissionService.class).getPermissionsByUserId(42L))
                .thenThrow(new org.mybatis.spring.MyBatisSystemException("权限读取失败", new org.springframework.dao.QueryTimeoutException("权限查询超时")));
        mvc.perform(get("/articles").header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable()).andExpect(header().exists("Retry-After"));
        verifyNoInteractions(context.getBean(ArticleService.class));
        mvc.perform(get("/articles")).andExpect(status().isUnauthorized());
    }

    @Configuration(proxyBeanMethods=false)
    @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class, CorsConfig.class, AuthController.class, ArticleController.class, ApiController.class,
            GlobalExceptionHandler.class})
    static class Config {
        @Bean ObjectMapper json() { return new ObjectMapper(); }
        @Bean com.nebula.common.util.SecurityUtils securityUtils() { return new com.nebula.common.util.SecurityUtils(); }
        @Bean RequestProtectionProperties limits() { return new RequestProtectionProperties(); }
        @Bean RequestAdmissionGuard admission(RequestProtectionProperties limits) { return new RequestAdmissionGuard(limits); }
        @Bean ClientIpResolver ips() { return new ClientIpResolver(List.of()); }
        @Bean ResourceGuard resources() { return mock(ResourceGuard.class); }
        @Bean UserMapper users() { return mock(UserMapper.class); }
        @Bean RoleService roles() { return mock(RoleService.class); }
        @Bean PermissionService permissions() { return mock(PermissionService.class); }
        @Bean TokenSessionService sessions() { return mock(TokenSessionService.class); }
        @Bean JwtUtil jwt(TokenSessionService sessions) { return new JwtUtil(sessions, Base64.getEncoder().encodeToString(io.jsonwebtoken.Jwts.SIG.HS256.key().build().getEncoded()),7200000); }
        @Bean JwtAuthenticationFilter jwtFilter(JwtUtil jwt,UserMapper users,RoleService roles,PermissionService permissions) { return new JwtAuthenticationFilter(jwt,users,roles,permissions); }
        @Bean JwtAccessDeniedHandler denied() { return new JwtAccessDeniedHandler(); }
        @Bean JwtAuthenticationEntryPoint unauthorized() { return new JwtAuthenticationEntryPoint(); }
        @Bean UserDetailsService userDetails() { return mock(UserDetailsService.class); }
        @Bean EmailAuthenticationProvider email() { return mock(EmailAuthenticationProvider.class); }
        @Bean PhoneAuthenticationProvider phone() { return mock(PhoneAuthenticationProvider.class); }
        @Bean AuthService auth() { return mock(AuthService.class); }
        @Bean AuthAbuseProtection abuse() { return mock(AuthAbuseProtection.class); }
        @Bean ArticleService articles() { return mock(ArticleService.class); }
        @Bean UserProfileFacade profiles() { return mock(UserProfileFacade.class); }
        @Bean TagHomeFacade tags() { return mock(TagHomeFacade.class); }
    }
}
