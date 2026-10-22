package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.auth.security.AuthAbuseProperties;
import com.nebula.auth.security.AuthRateLimitException;
import com.nebula.auth.security.RegistrationUnavailableException;
import com.nebula.auth.service.AuthAbuseProtection;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.service.LoginLogService;
import com.nebula.auth.service.RegistrationTokenService;
import com.nebula.auth.util.JwtUtil;
import com.nebula.common.constant.redisKey.AuthRedisKey;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.mail.service.MailService;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"nebula.security.abuse.login-ip-requests=3", "nebula.security.abuse.credential-attempts=3",
        "nebula.security.abuse.email-ip-requests=3", "nebula.security.abuse.verify-ip-requests=3",
        "nebula.security.abuse.email-mailbox-requests=3", "nebula.security.abuse.captcha-max-failures=3"})
@AutoConfigureMockMvc
@Transactional
class AuthAbuseIntegrationTest extends JwtIntegrationTestSupport {
    private static final String PREFIX = "auth:abuse:fixture:" + UUID.randomUUID() + ":";
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("nebula.security.abuse.key-prefix", () -> PREFIX); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthService auth;
    @Autowired AuthAbuseProtection guard;
    @Autowired RegistrationTokenService credentials;
    @Autowired AuthAbuseProperties limits;
    @Autowired RedisTemplate<String, Object> redis;
    @Autowired StringRedisTemplate strings;
    @Autowired UserMapper users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtUtil jwt;
    @MockitoBean MailService mail;
    @MockitoBean LoginLogService logs;
    private final List<String> emails = new ArrayList<>();
    private final List<String> sessions = new ArrayList<>();

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        sessions.forEach(jwt::revokeToken);
        var keys = strings.keys(PREFIX + "*");
        if (keys != null && !keys.isEmpty()) strings.delete(keys);
        for (String email : emails) for (String prefix : List.of(AuthRedisKey.EMAIL_CAPTCHA, AuthRedisKey.EMAIL_CAPTCHA_CD,
                AuthRedisKey.EMAIL_CAPTCHA_ATTEMPTS, AuthRedisKey.EMAIL_CAPTCHA_OWNER, AuthRedisKey.EMAIL_SEND_WINDOW)) redis.delete(prefix + email);
    }

    private String email() { String email = UUID.randomUUID() + "@example.invalid"; emails.add(email); return email; }
    private User fixture() {
        var user = User.create().setNid("abuse_" + UUID.randomUUID()).setEmail(email()).setUsername("Abuse fixture")
                .setPassword(passwords.encode("fixture-password")).setStatus(0).setDeleted(0);
        users.insert(user);
        return user;
    }
    private void login(String account, String password, String ip, int http) throws Exception {
        var result = mvc.perform(post("/auth/login").with(request -> { request.setRemoteAddr(ip); return request; })
                        .header("X-Forwarded-For", UUID.randomUUID().toString()).header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("account", account, "password", password))))
                .andExpect(status().is(http)).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        String token = body.path("data").path("token").asText("");
        if (!token.isBlank()) sessions.add(token);
        if (http == 429) {
            assertEquals(429, body.path("code").asInt());
            assertNotNull(result.getResponse().getHeader("Retry-After"));
            assertTrue(result.getResponse().getHeader("Access-Control-Expose-Headers").contains("Retry-After"));
        }
    }
    private <T> List<T> parallel(int count, Callable<T> action) throws Exception {
        var pool = Executors.newFixedThreadPool(count);
        var ready = new CountDownLatch(count);
        var start = new CountDownLatch(1);
        try {
            var tasks = new ArrayList<java.util.concurrent.Future<T>>();
            for (int i = 0; i < count; i++) tasks.add(pool.submit(() -> { ready.countDown(); assertTrue(start.await(10, TimeUnit.SECONDS)); return action.call(); }));
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            var results = new ArrayList<T>();
            for (var task : tasks) results.add(task.get(20, TimeUnit.SECONDS));
            return results;
        } finally { start.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS)); }
    }

    @Test void changingForwardedHeadersAndAccountsCannotBypassTheSourceLimit() throws Exception {
        for (int i = 0; i < 3; i++) login("missing_" + UUID.randomUUID(), "wrong", "198.51.100.20", 401);
        login("missing_" + UUID.randomUUID(), "wrong", "198.51.100.20", 429);
        verifyNoInteractions(mail);
    }

    @Test void usernameAndEmailShareAnAccountBudgetAcrossDifferentSources() throws Exception {
        var user = fixture();
        login(user.getNid(), "wrong", "198.51.100.1", 401);
        login(user.getEmail(), "wrong", "198.51.100.2", 401);
        login(user.getNid(), "wrong", "198.51.100.3", 401);
        login(user.getEmail(), "fixture-password", "198.51.100.4", 429);
        var keys = strings.keys(PREFIX + "login:*"); assertNotNull(keys); assertEquals(1, keys.size());
        long ttl = strings.getExpire(keys.iterator().next(), TimeUnit.SECONDS); assertTrue(ttl > 0 && ttl <= limits.getCredentialWindowSeconds());
        strings.delete(keys);
        login(user.getNid(), "fixture-password", "198.51.100.5", 200);
    }

    @Test void successfulAuthenticationResetsOnlyItsAccountBudget() throws Exception {
        var user = fixture();
        login(user.getNid(), "wrong", "198.51.100.1", 401);
        login(user.getNid(), "wrong", "198.51.100.2", 401);
        login(user.getNid(), "fixture-password", "198.51.100.3", 200);
        login(user.getNid(), "wrong", "198.51.100.4", 401);
        assertEquals("1", strings.opsForValue().get(strings.keys(PREFIX + "login:*").iterator().next()));
    }

    @Test void concurrentAttemptsCannotOverrunTheConfiguredBudget() throws Exception {
        var results = parallel(12, () -> { try { guard.requireCredentialAttempt("login", "parallel-fixture"); return true; }
            catch (AuthRateLimitException e) { return false; } });
        assertEquals(3, results.stream().filter(Boolean::booleanValue).count());
        String key = strings.keys(PREFIX + "login:*").iterator().next();
        assertEquals("3", strings.opsForValue().get(key));
        long before = strings.getExpire(key, TimeUnit.MILLISECONDS);
        assertThrows(AuthRateLimitException.class, () -> guard.requireCredentialAttempt("login", "parallel-fixture"));
        assertTrue(strings.getExpire(key, TimeUnit.MILLISECONDS) <= before);
    }

    @Test void simultaneousCaptchaSendsHaveExactlyOneMailWinner() throws Exception {
        String email = email();
        var results = parallel(8, () -> { try { return auth.sendEmailCaptcha(email) > 0; }
            catch (AuthRateLimitException e) { return false; } });
        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        verify(mail, times(1)).sendText(eq(email), anyString(), anyString());
        assertTrue(redis.getExpire(AuthRedisKey.EMAIL_CAPTCHA + email) > 0);
        assertTrue(redis.getExpire(AuthRedisKey.EMAIL_CAPTCHA_CD + email) > 0);
    }

    @Test void sourceQuotaCoversDifferentEmailRecipients() throws Exception {
        for (int i = 0; i < 4; i++) {
            mvc.perform(post("/auth/email/captcha").with(request -> { request.setRemoteAddr("198.51.100.40"); return request; })
                            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email", email()))))
                    .andExpect(status().is(i < 3 ? 200 : 429));
        }
        verify(mail, times(3)).sendText(anyString(), anyString(), anyString());
    }

    @Test void mailboxQuotaSurvivesCooldownExpiration() {
        String email = email();
        for (int i = 0; i < 3; i++) { assertTrue(auth.sendEmailCaptcha(email) > 0); redis.delete(AuthRedisKey.EMAIL_CAPTCHA_CD + email); }
        assertThrows(AuthRateLimitException.class, () -> auth.sendEmailCaptcha(email));
        verify(mail, times(3)).sendText(eq(email), anyString(), anyString());
        assertEquals(3, ((Number) redis.opsForValue().get(AuthRedisKey.EMAIL_SEND_WINDOW + email)).intValue());
    }

    @Test void delayedMailFailureCannotInvalidateANewerChallenge() {
        String email = email();
        String originalOwner = credentials.issueCaptcha(email, "123456");
        redis.delete(AuthRedisKey.EMAIL_CAPTCHA_CD + email);
        credentials.issueCaptcha(email, "654321");
        credentials.discardCaptcha(email, originalOwner);
        assertDoesNotThrow(() -> credentials.requireCaptcha(email, "654321"));
        assertEquals("654321", redis.opsForValue().get(AuthRedisKey.EMAIL_CAPTCHA + email));
    }

    @Test void failedMailDoesNotReportSuccessOrLeaveAUsableCode() throws Exception {
        String email = email();
        doThrow(new MailSendException("private SMTP detail")).when(mail).sendText(eq(email), anyString(), anyString());
        mvc.perform(post("/auth/email/captcha").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("注册服务暂时不可用，请稍后重试"));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(AuthRedisKey.EMAIL_CAPTCHA + email)));
        assertTrue(Boolean.TRUE.equals(redis.hasKey(AuthRedisKey.EMAIL_CAPTCHA_CD + email)));
        assertThrows(AuthRateLimitException.class, () -> auth.sendEmailCaptcha(email));
        verify(mail, times(1)).sendText(eq(email), anyString(), anyString());
    }

    @Test void tooManyIncorrectCodesInvalidateTheChallengeEvenWhenTheNextCodeIsCorrect() {
        String email = email(); credentials.issueCaptcha(email, "123456");
        for (int i = 0; i < 3; i++) {
            int expected = i < 2 ? 40010 : 40012;
            assertEquals(expected, assertThrows(BusinessException.class, () -> credentials.requireCaptcha(email, "000000")).getCode());
        }
        assertFalse(Boolean.TRUE.equals(redis.hasKey(AuthRedisKey.EMAIL_CAPTCHA + email)));
        assertEquals(40010, assertThrows(BusinessException.class, () -> credentials.verifyAndIssue(email, "123456", "hash")).getCode());
        verifyNoInteractions(mail);
    }

    @Test void concurrentIncorrectCodesCannotRaceTheFailureLimit() throws Exception {
        String email = email(); credentials.issueCaptcha(email, "123456");
        parallel(12, () -> { try { credentials.requireCaptcha(email, "000000"); return false; } catch (BusinessException e) { return true; } });
        assertFalse(Boolean.TRUE.equals(redis.hasKey(AuthRedisKey.EMAIL_CAPTCHA + email)));
        assertEquals(3, ((Number) redis.opsForValue().get(AuthRedisKey.EMAIL_CAPTCHA_ATTEMPTS + email)).intValue());
        verifyNoInteractions(mail);
    }

    @Test void verificationHasASourceLimitAcrossMailboxes() throws Exception {
        for (int i = 0; i < 4; i++) {
            String email = email(); credentials.issueCaptcha(email, "123456");
            mvc.perform(post("/auth/email/verify").with(request -> { request.setRemoteAddr("198.51.100.60"); return request; })
                            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email", email, "code", "000000", "password", "unused-password"))))
                    .andExpect(status().is(i < 3 ? 400 : 429));
        }
        verifyNoInteractions(mail);
    }
}
