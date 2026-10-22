package com.nebula.start;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.auth.entity.LoginLog;
import com.nebula.auth.mapper.LoginLogMapper;
import com.nebula.auth.security.AuthLoginUser;
import com.nebula.auth.token.PhoneAuthenticationToken;
import com.nebula.auth.util.JwtUtil;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.nebula.auth.entity.table.LoginLogTableDef.LOGIN_LOG;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real providers, logging, MySQL/Flex, Redis sessions and Security; database writes roll back. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginIntegrityIntegrationTest extends JwtIntegrationTestSupport {
    private static final String PASSWORD = "login-integrity-fixture-password";
    private static final String DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
    private static final String MOBILE = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
    @Autowired UserMapper users;
    @Autowired LoginLogMapper logs;
    @Autowired PasswordEncoder passwords;
    @Autowired AuthenticationManager authenticationManager;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoSpyBean JwtUtil jwt;
    private final List<String> tokens = new ArrayList<>();

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        tokens.forEach(jwt::revokeToken);
    }

    private User fixture(int status) {
        String marker = UUID.randomUUID().toString().replace("-", "");
        User user = User.create().setNid("login_" + marker).setUsername("Login integrity fixture")
                .setEmail(marker + "@example.invalid").setPhone("t_" + marker.substring(0, 16))
                .setPassword(passwords.encode(PASSWORD)).setStatus(status).setDeleted(0);
        assertEquals(1, users.insert(user));
        return user;
    }

    private JsonNode login(User user, boolean email, String password, String userAgent) throws Exception {
        SecurityContextHolder.clearContext();
        var request = post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("account", email ? user.getEmail() : user.getNid(), "password", password)));
        if (userAgent != null) request.header("User-Agent", userAgent);
        var response = mvc.perform(request).andReturn().getResponse();
        var body = json.readTree(response.getContentAsString());
        assertEquals(body.path("code").asInt() == 200 ? 200 : 401, response.getStatus());
        String token = body.path("data").path("token").asText("");
        if (!token.isBlank()) tokens.add(token);
        return body;
    }

    private LoginLog log(User user, int expectedStatus) {
        var records = logs.selectListByCondition(LOGIN_LOG.USER_ID.eq(user.getId()));
        assertEquals(1, records.size());
        var log = records.get(0);
        assertEquals(expectedStatus, log.getStatus());
        return log;
    }

    private void usableSession(User user, JsonNode body) throws Exception {
        assertEquals(200, body.path("code").asInt());
        String token = body.path("data").path("token").asText();
        assertFalse(token.isBlank());
        assertTrue(jwt.isSessionActive(jwt.parseToken(token), user));
        SecurityContextHolder.clearContext();
        mvc.perform(get("/users/" + user.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @ParameterizedTest
    @CsvSource({"true,1,4", "true,2,7", "false,1,4", "false,2,7", "true,99,4", "false,99,4"})
    void restrictedAccountsFailBeforeTokenIssuanceAndStillRecordTheReason(boolean email, int state, int expectedCode) throws Exception {
        var user = fixture(state);
        var body = login(user, email, PASSWORD, null);
        assertEquals(expectedCode, body.path("code").asInt());
        assertTrue(body.path("data").path("token").isMissingNode());
        assertEquals("Unknown", log(user, expectedCode).getUserAgent());
        verify(jwt, never()).createToken(any(User.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void normalPasswordLoginsKeepDesktopMetadata(boolean email) throws Exception {
        var user = fixture(0);
        usableSession(user, login(user, email, PASSWORD, DESKTOP));
        var record = log(user, 1);
        assertEquals(DESKTOP, record.getUserAgent());
        assertEquals("Chrome", record.getBrowser());
        assertTrue(record.getOs().startsWith("Windows 10"));
        assertEquals("PC", record.getDevice());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not-a-browser-@@@"})
    void missingOrUnrecognizedUserAgentsDoNotPreventEmailLogin(String userAgent) throws Exception {
        var user = fixture(0);
        usableSession(user, login(user, true, PASSWORD, userAgent));
        var record = log(user, 1);
        assertEquals(userAgent == null || userAgent.isBlank() ? "Unknown" : userAgent, record.getUserAgent());
        assertEquals("Unknown", record.getBrowser());
        assertEquals("Unknown", record.getOs());
        assertEquals("Unknown", record.getDevice());
    }

    @Test
    void mobileMetadataRemainsAvailable() throws Exception {
        var user = fixture(0);
        usableSession(user, login(user, false, PASSWORD, MOBILE));
        var record = log(user, 1);
        assertEquals("Mobile", record.getDevice());
        assertNotEquals("Unknown", record.getBrowser());
        assertNotEquals("Unknown", record.getOs());
    }

    @Test
    void oversizedUserAgentsFitTheExistingDatabaseColumn() throws Exception {
        var user = fixture(0);
        usableSession(user, login(user, true, PASSWORD, DESKTOP + "X".repeat(2000)));
        var record = log(user, 1);
        assertEquals(512, record.getUserAgent().length());
        assertEquals("PC", record.getDevice());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void wrongPasswordsWithoutUserAgentsRemainAuthenticationFailures(boolean email) throws Exception {
        var user = fixture(0);
        var body = login(user, email, "wrong-password", null);
        assertEquals(3, body.path("code").asInt());
        assertEquals("Unknown", log(user, 3).getBrowser());
        verify(jwt, never()).createToken(any(User.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deletedAccountsCannotObtainTokens(boolean email) throws Exception {
        var user = fixture(0);
        assertEquals(1, users.deleteById(user.getId()));
        var body = login(user, email, PASSWORD, null);
        assertNotEquals(200, body.path("code").asInt());
        assertTrue(body.path("data").path("token").isMissingNode());
        assertEquals(0, logs.selectCountByCondition(LOGIN_LOG.USER_ID.eq(user.getId())));
        verify(jwt, never()).createToken(any(User.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 99})
    void phoneProviderEnforcesTheSameAccountStates(int state) {
        var user = fixture(state);
        var request = new PhoneAuthenticationToken(user.getPhone(), PASSWORD);
        if (state == 0) {
            var result = authenticationManager.authenticate(request);
            assertTrue(result.isAuthenticated());
            assertEquals(user.getId(), ((AuthLoginUser) result.getPrincipal()).getUserId());
        } else if (state == 2) {
            assertThrows(LockedException.class, () -> authenticationManager.authenticate(request));
        } else {
            assertThrows(DisabledException.class, () -> authenticationManager.authenticate(request));
        }
        verify(jwt, never()).createToken(any(User.class));
    }
}
