package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.auth.service.LoginLogService;
import com.nebula.auth.service.TokenSessionService;
import com.nebula.auth.util.JwtUtil;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.dto.ChangePasswordDTO;
import com.nebula.auth.security.AuthLoginUser;
import com.nebula.common.constant.LoginStatus;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Full Security chain, real Redis, and MyBatis-Flex writes rolled back after each test. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TokenRevocationIntegrationTest extends JwtIntegrationTestSupport {
    @Autowired UserMapper users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtUtil jwt;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthService auth;
    @Autowired TokenSessionService sessions;
    @MockitoBean LoginLogService loginLogs; // Avoid unrelated geolocation/network calls.
    @Value("${jwt.secret}") String signingSecret;
    private final List<String> tokens = new ArrayList<>();

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        tokens.forEach(jwt::revokeToken);
    }

    private User fixture() {
        var user = User.create().setNid("revoke_" + UUID.randomUUID())
                .setUsername("Token revocation fixture")
                .setPassword(passwords.encode("fixture-old-password")).setStatus(0).setDeleted(0);
        users.insert(user);
        return user;
    }

    private String login(User user, String password) throws Exception {
        SecurityContextHolder.clearContext();
        var result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Nebula session regression")
                        .content(json.writeValueAsString(Map.of("account", user.getNid(), "password", password))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andReturn();
        String token = json.readTree(result.getResponse().getContentAsString()).path("data").path("token").asText();
        assertFalse(token.isBlank());
        tokens.add(token);
        return token;
    }

    private String issue(User user) {
        String token = jwt.createToken(user);
        tokens.add(token);
        return token;
    }

    private void accessible(User user, String token, boolean allowed) throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/users/" + user.getId()).header("Authorization", "Bearer " + token))
                .andExpect(allowed ? status().isOk() : status().isUnauthorized());
    }

    private void change(String token, String oldPassword, String newPassword, String confirm, int code) throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(post("/auth/change-password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "oldPassword", oldPassword, "newPassword", newPassword, "confirmPassword", confirm))))
                .andExpect(code == 200 ? status().isOk() : status().isBadRequest())
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void logoutRevokesOnlyTheCurrentSessionAndReplayedTokenIsUnauthorized() throws Exception {
        User owner = fixture();
        User other = fixture();
        String first = login(owner, "fixture-old-password");
        String second = login(owner, "fixture-old-password");
        String otherToken = issue(other);
        assertNotEquals(first, second, "Each login needs an independent session even within the same second");
        accessible(owner, first, true);
        SecurityContextHolder.clearContext();
        mvc.perform(post("/auth/logout").header("Authorization", "Bearer " + first))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        accessible(owner, first, false);
        accessible(owner, second, true);
        accessible(other, otherToken, true);
        accessible(owner, login(owner, "fixture-old-password"), true);
        SecurityContextHolder.clearContext();
        mvc.perform(post("/auth/logout").header("Authorization", "Bearer " + first))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordChangeInvalidatesAllPriorSessionsButNotOtherUsers() throws Exception {
        User owner = fixture();
        User other = fixture();
        String first = login(owner, "fixture-old-password");
        String second = login(owner, "fixture-old-password");
        String otherToken = issue(other);
        change(first, "fixture-old-password", "fixture-new-password", "fixture-new-password", 200);
        assertTrue(passwords.matches("fixture-new-password", users.selectOneById(owner.getId()).getPassword()));
        accessible(owner, first, false);
        accessible(owner, second, false);
        accessible(other, otherToken, true);
        SecurityContextHolder.clearContext();
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("account", owner.getNid(), "password", "fixture-old-password"))))
                .andExpect(jsonPath("$.code").value(LoginStatus.PASSWORD_ERROR.getCode()));
        String fresh = login(owner, "fixture-new-password");
        accessible(owner, fresh, true);
        // Returning to a previous plaintext password must not resurrect its old sessions.
        change(fresh, "fixture-new-password", "fixture-old-password", "fixture-old-password", 200);
        accessible(owner, first, false);
        accessible(owner, second, false);
        accessible(owner, fresh, false);
        accessible(owner, login(owner, "fixture-old-password"), true);
    }

    @Test
    void failedPasswordChecksDoNotInvalidateOtherSessions() throws Exception {
        User owner = fixture();
        String first = issue(owner);
        String second = issue(owner);
        change(first, "wrong-password", "fixture-new-password", "fixture-new-password", 40020);
        change(first, "fixture-old-password", "fixture-new-password", "mismatched", 40021);
        change(first, "", "fixture-new-password", "fixture-new-password", 400);
        assertEquals(owner.getPassword(), users.selectOneById(owner.getId()).getPassword());
        accessible(owner, first, true);
        accessible(owner, second, true);
    }

    @Test
    void evenReusingTheSamePlaintextPasswordInvalidatesExistingSessions() throws Exception {
        User owner = fixture();
        String first = issue(owner);
        String second = issue(owner);
        change(first, "fixture-old-password", "fixture-old-password", "fixture-old-password", 200);
        assertNotEquals(owner.getPassword(), users.selectOneById(owner.getId()).getPassword());
        accessible(owner, first, false);
        accessible(owner, second, false);
    }

    @Test
    void anonymousLogoutAndInvalidSignaturesCannotReachProtectedEndpoints() throws Exception {
        User owner = fixture();
        SecurityContextHolder.clearContext();
        mvc.perform(post("/auth/logout")).andExpect(status().isUnauthorized());
        accessible(owner, "not.a.jwt", false);
        String token = issue(owner);
        String[] parts = token.split("\\.");
        parts[2] = (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        accessible(owner, String.join(".", parts), false);
    }

    @Test
    void legacyAndUnregisteredSignedTokensAreRejectedWithoutChangingSigningKey() throws Exception {
        User owner = fixture();
        var key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(signingSecret));
        String legacy = Jwts.builder().subject(owner.getId().toString()).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000)).signWith(key).compact();
        String unregistered = Jwts.builder().subject(owner.getId().toString()).id(UUID.randomUUID().toString())
                .expiration(new Date(System.currentTimeMillis() + 60_000)).signWith(key).compact();
        accessible(owner, legacy, false);
        accessible(owner, unregistered, false);
        accessible(owner, issue(owner), true);
    }

    @Test
    void tokensSignedByRetiredKeysAreRejectedEvenWhenTheirRedisSessionsAreActive() throws Exception {
        User owner = fixture();
        String retiredSecret = Base64.getEncoder().encodeToString(Jwts.SIG.HS256.key().build().getEncoded());
        var retiredJwt = new JwtUtil(sessions, retiredSecret, 60_000);
        String oldToken = retiredJwt.createToken(owner);
        try {
            assertTrue(retiredJwt.isSessionActive(retiredJwt.parseToken(oldToken), owner));
            accessible(owner, oldToken, false);
            accessible(owner, issue(owner), true);
        } finally {
            retiredJwt.revokeToken(oldToken);
        }
    }

    @Test
    void anAlreadyAuthenticatedStaleRequestCannotChangePasswordAgain() throws Exception {
        User owner = fixture();
        var stalePrincipal = new AuthLoginUser(owner, List.of(), List.of());
        change(issue(owner), "fixture-old-password", "fixture-new-password", "fixture-new-password", 200);
        String committedHash = users.selectOneById(owner.getId()).getPassword();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(stalePrincipal, null, List.of()));
        var staleChange = new ChangePasswordDTO();
        staleChange.setOldPassword("fixture-old-password");
        staleChange.setNewPassword("stale-request-password");
        staleChange.setConfirmPassword("stale-request-password");
        assertThrows(RuntimeException.class, () -> auth.changePassword(staleChange));
        assertEquals(committedHash, users.selectOneById(owner.getId()).getPassword());
    }
}
