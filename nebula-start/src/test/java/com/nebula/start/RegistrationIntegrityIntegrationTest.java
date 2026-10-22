package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.row.Db;
import com.nebula.auth.dto.EmailVerifyDTO;
import com.nebula.auth.dto.RegisterCacheDTO;
import com.nebula.auth.dto.RegisterDTO;
import com.nebula.auth.security.RegistrationUnavailableException;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.service.LoginLogService;
import com.nebula.auth.service.RegistrationTokenService;
import com.nebula.auth.util.JwtUtil;
import com.nebula.common.constant.RegisterType;
import com.nebula.common.constant.redisKey.AuthRedisKey;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.mail.service.MailService;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.nebula.user.entity.table.UserTableDef.USER;
import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real MySQL/Flex, Redis and Security. Only random test accounts are cleaned up. No mail is sent. */
@SpringBootTest
@AutoConfigureMockMvc
class RegistrationIntegrityIntegrationTest extends JwtIntegrationTestSupport {
    @Autowired AuthService auth;
    @Autowired RegistrationTokenService credentials;
    @Autowired UserMapper users;
    @MockitoSpyBean UserRoleMapper userRoles;
    @Autowired RedisTemplate<String, Object> redis;
    @Autowired PasswordEncoder passwords;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtUtil jwt;
    @MockitoBean MailService mail;
    @MockitoBean LoginLogService loginLogs;
    private final List<String> emails = new ArrayList<>();
    private final List<String> keys = new ArrayList<>();
    private final List<String> sessions = new ArrayList<>();

    private String email() {
        String email = "registration_" + UUID.randomUUID().toString().replace("-", "") + "@example.invalid";
        emails.add(email);
        return email;
    }

    private String key(String prefix, String suffix) {
        String key = prefix + suffix;
        keys.add(key);
        return key;
    }

    private String grant(String email, RegisterType type) {
        String token = UUID.randomUUID().toString().replace("-", "");
        RegisterCacheDTO cache = new RegisterCacheDTO();
        cache.setAccount(email);
        cache.setPassword(passwords.encode("registration-test-password"));
        cache.setType(type);
        redis.opsForValue().set(key(AuthRedisKey.REGISTER_TOKEN, token), cache, Duration.ofMinutes(10));
        return token;
    }

    private RegisterDTO registration(String token) { return new RegisterDTO().setToken(token); }

    private void captcha(String email) {
        key(AuthRedisKey.EMAIL_CAPTCHA_ATTEMPTS, email);
        key(AuthRedisKey.EMAIL_CAPTCHA_OWNER, email);
        redis.opsForValue().set(key(AuthRedisKey.EMAIL_CAPTCHA, email), "123456", Duration.ofMinutes(5));
    }

    private EmailVerifyDTO verification(String email, String code) {
        var dto = new EmailVerifyDTO();
        dto.setEmail(email);
        dto.setCode(code);
        dto.setPassword("registration-test-password");
        return dto;
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        sessions.forEach(jwt::revokeToken);
        keys.forEach(redis::delete);
        for (String email : emails) {
            var user = users.selectOneByCondition(USER.EMAIL.eq(email));
            if (user != null) {
                Db.deleteByMap("user_role", Map.of("user_id", user.getId()));
                Db.deleteByMap("login_log", Map.of("user_id", user.getId()));
                Db.deleteById("user", "id", user.getId());
            }
            assertEquals(0, users.selectCountByCondition(USER.EMAIL.eq(email)));
        }
    }

    private <T> List<T> parallel(int count, Callable<T> action) throws Exception {
        var pool = Executors.newFixedThreadPool(count);
        var ready = new CountDownLatch(count);
        var start = new CountDownLatch(1);
        try {
            var tasks = new ArrayList<java.util.concurrent.Future<T>>();
            for (int i = 0; i < count; i++) tasks.add(pool.submit(() -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return action.call();
            }));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var results = new ArrayList<T>();
            for (var task : tasks) results.add(task.get(20, TimeUnit.SECONDS));
            return results;
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void databaseEnforcesEmailUniquenessAcrossAllWriters() {
        var indexes = Db.selectListBySql("SHOW INDEX FROM `user`");
        assertTrue(indexes.stream().anyMatch(row -> "email".equals(row.get("Column_name"))
                && ((Number) row.get("Non_unique")).intValue() == 0
                && indexes.stream().filter(other -> other.get("Key_name").equals(row.get("Key_name"))).count() == 1));
    }

    @Test
    void verificationRegistrationAndEmailLoginWorkThroughAnonymousEndpoints() throws Exception {
        String email = email();
        captcha(email);
        mvc.perform(post("/auth/email/verify").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "  " + email.toUpperCase(Locale.ROOT) + "  ",
                                "code", "123456", "password", "registration-test-password"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        var body = ArgumentCaptor.forClass(String.class);
        verify(mail).sendHtml(eq(email), eq("Nebula Blog 注册确认"), body.capture());
        var match = Pattern.compile("register\\?token=([a-f0-9]{32})").matcher(body.getValue());
        assertTrue(match.find());
        String token = match.group(1);
        key(AuthRedisKey.REGISTER_TOKEN, token);
        assertFalse(Boolean.TRUE.equals(redis.hasKey(AuthRedisKey.EMAIL_CAPTCHA + email)));
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("token", token))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
        var user = users.selectOneByCondition(USER.EMAIL.eq(email));
        assertNotNull(user);
        assertEquals(email, user.getEmail());
        assertTrue(passwords.matches("registration-test-password", user.getPassword()));
        assertEquals(user.getId().toString(), user.getNid());
        assertEquals(0, user.getStatus());
        assertEquals(List.of(3L), userRoles.selectListByCondition(USER_ROLE.USER_ID.eq(user.getId()))
                .stream().map(UserRole::getRoleId).toList());
        assertFalse(Boolean.TRUE.equals(redis.hasKey(AuthRedisKey.REGISTER_TOKEN + token)));
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("token", token))))
                .andExpect(jsonPath("$.code").value(40011));
        assertEquals(1, users.selectCountByCondition(USER.EMAIL.eq(email)));
        var loggedIn = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("account", email, "password", "registration-test-password"))))
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.roles[0]").value("USER"))
                .andReturn();
        String bearer = json.readTree(loggedIn.getResponse().getContentAsString()).path("data").path("token").asText();
        sessions.add(bearer);
        mvc.perform(get("/api/profile/" + user.getId()).header("Authorization", "Bearer " + bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userProfileInfoVO.email").value(email));
    }

    @Test
    void oneCaptchaCanIssueOnlyOneLinkUnderConcurrentVerification() throws Exception {
        String email = email();
        captcha(email);
        String encoded = passwords.encode("registration-test-password");
        var results = parallel(8, () -> {
            try { return credentials.verifyAndIssue(email, "123456", encoded); }
            catch (BusinessException e) { assertEquals(40010, e.getCode()); return "rejected"; }
        });
        var issued = results.stream().filter(value -> !value.equals("rejected")).toList();
        issued.forEach(token -> key(AuthRedisKey.REGISTER_TOKEN, token));
        assertEquals(1, issued.size());
    }

    @Test
    void oneLinkCreatesOnlyOneAccountUnderConcurrentRequests() throws Exception {
        String email = email();
        String token = grant(email, RegisterType.EMAIL);
        var results = parallel(8, () -> {
            try { assertTrue(auth.register(registration(token))); return 200; }
            catch (BusinessException e) { return e.getCode(); }
        });
        assertEquals(1, results.stream().filter(code -> code == 200).count());
        assertEquals(7, results.stream().filter(code -> code == 40011).count());
        assertEquals(1, users.selectCountByCondition(USER.EMAIL.eq(email)));
    }

    @Test
    void differentLinksForTheSameEmailCannotBypassTheDatabaseConstraint() throws Exception {
        String email = email();
        var links = new java.util.concurrent.ConcurrentLinkedQueue<>(List.of(
                grant(email, RegisterType.EMAIL), grant(email.toUpperCase(Locale.ROOT), RegisterType.EMAIL)));
        var results = parallel(2, () -> {
            try { assertTrue(auth.register(registration(links.remove()))); return 200; }
            catch (BusinessException e) { return e.getCode(); }
        });
        assertEquals(1, results.stream().filter(code -> code == 200).count());
        assertEquals(1, results.stream().filter(code -> code == 40910).count());
        assertEquals(1, users.selectCountByCondition(USER.EMAIL.eq(email)));
    }

    @Test
    void sqlRollbackRestoresTheOriginalLinkWithoutExtendingItsLifetime() {
        String email = email();
        String token = grant(email, RegisterType.EMAIL);
        long before = redis.getExpire(AuthRedisKey.REGISTER_TOKEN + token, TimeUnit.MILLISECONDS);
        doReturn(0).when(userRoles).insertSelective(any(UserRole.class));
        assertThrows(RegistrationUnavailableException.class, () -> auth.register(registration(token)));
        assertEquals(0, users.selectCountByCondition(USER.EMAIL.eq(email)));
        long after = redis.getExpire(AuthRedisKey.REGISTER_TOKEN + token, TimeUnit.MILLISECONDS);
        assertTrue(after > 0 && after <= before);
        reset(userRoles);
        assertTrue(auth.register(registration(token)));
        assertNotNull(users.selectOneByCondition(USER.EMAIL.eq(email)));
    }

    @Test
    void invalidCaptchaDoesNotConsumeTheRealCodeOrSendMail() {
        String email = email();
        captcha(email);
        var failure = assertThrows(BusinessException.class, () -> auth.verifyEmailCaptcha(verification(email, "wrong")));
        assertEquals(40010, failure.getCode());
        assertEquals("123456", redis.opsForValue().get(AuthRedisKey.EMAIL_CAPTCHA + email));
        verifyNoInteractions(mail);
    }

    @Test
    void expiredMissingAndMalformedLinksCannotCreateAccounts() throws Exception {
        String email = email();
        String token = grant(email, RegisterType.EMAIL);
        redis.expire(AuthRedisKey.REGISTER_TOKEN + token, Duration.ZERO);
        assertEquals(40011, assertThrows(BusinessException.class, () -> auth.register(registration(token))).getCode());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"invalid\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        assertEquals(0, users.selectCountByCondition(USER.EMAIL.eq(email)));
    }

    @Test
    void legacyEmailLinksStillWorkButPhoneLinksCannotBecomeEmailAccounts() {
        String email = email();
        assertTrue(auth.register(registration(grant(email, null))));
        String other = email();
        var failure = assertThrows(BusinessException.class,
                () -> auth.register(registration(grant(other, RegisterType.PHONE))));
        assertEquals(40011, failure.getCode());
        assertEquals(0, users.selectCountByCondition(USER.EMAIL.eq(other)));
    }

    @Test
    void existingEmailIsRejectedWithoutSendingAnotherConfirmation() {
        String email = email();
        assertTrue(auth.register(registration(grant(email, RegisterType.EMAIL))));
        captcha(email);
        assertEquals(40910, assertThrows(BusinessException.class,
                () -> auth.verifyEmailCaptcha(verification(email.toUpperCase(Locale.ROOT), "123456"))).getCode());
        verifyNoInteractions(mail);
    }
}
