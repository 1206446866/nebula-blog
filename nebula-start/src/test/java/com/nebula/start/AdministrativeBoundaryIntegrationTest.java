package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.nebula.article.dto.ChangeArticleStatusDto;
import com.nebula.article.entity.Article;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.article.service.ArticleService;
import com.nebula.auth.AuthUserBuilder;
import com.nebula.auth.dto.ResetPasswordDTO;
import com.nebula.auth.mapper.LoginLogMapper;
import com.nebula.auth.service.AuthService;
import com.nebula.auth.util.JwtUtil;
import com.nebula.common.mail.service.MailService;
import com.nebula.common.redis.RedisService;
import com.nebula.role.entity.Role;
import com.nebula.role.entity.RolePermission;
import com.nebula.role.mapper.PermissionMapper;
import com.nebula.role.mapper.RoleMapper;
import com.nebula.role.mapper.RolePermissionMapper;
import com.nebula.user.entity.User;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.nebula.auth.entity.table.LoginLogTableDef.LOGIN_LOG;
import static com.nebula.role.entity.table.PermissionTableDef.PERMISSION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdministrativeBoundaryIntegrationTest extends JwtIntegrationTestSupport {
    private static final String PASSWORD = "fixture-old-password";
    private static final String NEW_PASSWORD = "fixture-new-password";
    private static final String PREFIX = "auth:abuse:admin-fixture:" + UUID.randomUUID() + ":";
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("nebula.security.abuse.key-prefix", () -> PREFIX); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoSpyBean UserMapper users;
    @Autowired UserRoleMapper userRoles;
    @Autowired RoleMapper roles;
    @Autowired PermissionMapper permissions;
    @Autowired RolePermissionMapper rolePermissions;
    @MockitoSpyBean ArticleMapper articles;
    @Autowired ArticleService articleService;
    @Autowired AuthService auth;
    @Autowired AuthUserBuilder principals;
    @Autowired LoginLogMapper logs;
    @Autowired JwtUtil jwt;
    @Autowired PasswordEncoder passwords;
    @Autowired StringRedisTemplate redis;
    @MockitoBean MailService mail;
    @MockitoBean RedisService cache;
    private final List<String> sessions = new ArrayList<>();

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        sessions.forEach(jwt::revokeToken);
        var keys = redis.keys(PREFIX + "*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
        verifyNoInteractions(mail);
    }

    private User user(Long roleId) {
        var user = User.create().setNid("admin_boundary_" + UUID.randomUUID()).setUsername("Administrative boundary fixture")
                .setPassword(passwords.encode(PASSWORD)).setStatus(0).setDeleted(0);
        users.insert(user);
        if (roleId != null) userRoles.insert(UserRole.create().setUserId(user.getId()).setRoleId(roleId));
        return user;
    }
    private User scopedRole(String roleName, String... allowed) {
        var role = Role.create().setName(roleName.substring(0, Math.min(50, roleName.length()))).setDescription("Boundary fixture").setDeleted(0);
        roles.insert(role);
        for (String name : allowed) {
            var permission = permissions.selectOneByCondition(PERMISSION.NAME.eq(name));
            assertNotNull(permission);
            rolePermissions.insert(RolePermission.create().setRoleId(role.getId()).setPermissionId(permission.getId()));
        }
        return user(role.getId());
    }
    private String bearer(User user) {
        String token = jwt.createToken(user); sessions.add(token); return "Bearer " + token;
    }
    private void authenticated(User actor) {
        var principal = principals.createLoginUser(actor);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    private ResetPasswordDTO reset(String operatorPassword, String password) {
        var dto = new ResetPasswordDTO(); dto.setOperatorPassword(operatorPassword); dto.setNewPassword(password); dto.setConfirmPassword(password); return dto;
    }
    private void reset(User actor, User target, ResetPasswordDTO dto, int http, int code) throws Exception {
        mvc.perform(post("/auth/reset-password/" + target.getId()).header("Authorization", bearer(actor))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dto)))
                .andExpect(status().is(http)).andExpect(jsonPath("$.code").value(code));
    }
    private Article article(User owner) {
        var article = Article.create().setUserId(owner.getId()).setAuthor(owner.getUsername()).setTitle("Status boundary fixture")
                .setContent("Isolated fixture").setStatus(0).setDeleted(0).setLikeCount(0).setViewCount(0L);
        articles.insert(article); return article;
    }

    @Test void onlyArticlePermissionCanAuthorizeAdministrativeStatusChanges() throws Exception {
        var admin = scopedRole("ROLE_ADMIN", "user:update");
        var article = article(admin);
        mvc.perform(put("/articles/status").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("id", article.getId(), "status", 1))))
                .andExpect(status().isForbidden());
        assertEquals(0, articles.selectOneById(article.getId()).getStatus());
        authenticated(admin);
        assertThrows(AccessDeniedException.class, () -> articleService.changeArticleStatus(ChangeArticleStatusDto.create().setId(article.getId()).setStatus(1)));
    }

    @Test void articleEditorsWithAdminRoleDoNotNeedUserManagementPermission() throws Exception {
        var admin = scopedRole("ROLE_ADMIN", "article:update");
        var article = article(admin);
        mvc.perform(put("/articles/status").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("id", article.getId(), "status", 1))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        assertEquals(1, articles.selectOneById(article.getId()).getStatus());
    }

    @Test void articlePermissionAloneDoesNotGiveAnOrdinaryUserAdministrativePublishing() throws Exception {
        var actor = scopedRole("BOUNDARY_EDITOR_" + UUID.randomUUID(), "article:update");
        var article = article(actor);
        mvc.perform(put("/articles/status").header("Authorization", bearer(actor)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("id", article.getId(), "status", 1))))
                .andExpect(status().isForbidden());
        assertEquals(0, articles.selectOneById(article.getId()).getStatus());
    }

    @Test void missingArticlesAndFailedStatusWritesCannotReportSuccess() throws Exception {
        var root = user(1L); var article = article(root);
        mvc.perform(put("/articles/status").header("Authorization", bearer(root)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("id", Long.MAX_VALUE, "status", 1))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(40401));
        doReturn(0).when(articles).update(any(Article.class));
        mvc.perform(put("/articles/status").header("Authorization", bearer(root)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("id", article.getId(), "status", 1))))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value(40406));
        assertEquals(0, articles.selectOneById(article.getId()).getStatus());
        verifyNoInteractions(cache);
    }

    @Test void rootPasswordResetRevokesAllTargetSessionsAndPreservesTheOperatorSession() throws Exception {
        var root = user(1L); var target = user(3L); String first = bearer(target); String second = bearer(target); String operator = bearer(root);
        mvc.perform(post("/auth/reset-password/" + target.getId()).header("Authorization", operator)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(reset(PASSWORD, NEW_PASSWORD))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        assertTrue(passwords.matches(NEW_PASSWORD, users.selectOneById(target.getId()).getPassword()));
        for (String old : List.of(first, second)) mvc.perform(get("/users/" + target.getId()).header("Authorization", old)).andExpect(status().isUnauthorized());
        mvc.perform(get("/users/" + root.getId()).header("Authorization", operator)).andExpect(status().isOk());
        var response = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("account", target.getNid(), "password", NEW_PASSWORD))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andReturn();
        sessions.add(json.readTree(response.getResponse().getContentAsString()).path("data").path("token").asText());
    }

    @Test void administratorsMayResetOnlyLowerPrivilegeAccounts() throws Exception {
        var admin = scopedRole("ROLE_ADMIN", "user:update"); var target = user(3L);
        reset(admin, target, reset(PASSWORD, NEW_PASSWORD), 200, 200);
        assertTrue(passwords.matches(NEW_PASSWORD, users.selectOneById(target.getId()).getPassword()));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2})
    void administratorsCannotResetTheirPeersOrSuperAdministrators(long role) throws Exception {
        var admin = scopedRole("ROLE_ADMIN", "user:update"); var target = user(role);
        reset(admin, target, reset(PASSWORD, NEW_PASSWORD), 403, 403);
        assertEquals(target.getPassword(), users.selectOneById(target.getId()).getPassword());
    }

    @Test void userUpdatePermissionDoesNotTurnAnOrdinaryUserIntoAPasswordAdministrator() throws Exception {
        var caller = scopedRole("BOUNDARY_USER_" + UUID.randomUUID(), "user:update"); var target = user(3L);
        reset(caller, target, reset(PASSWORD, NEW_PASSWORD), 403, 403);
        authenticated(caller);
        assertThrows(AccessDeniedException.class, () -> auth.resetPassword(target.getId(), reset(PASSWORD, NEW_PASSWORD)));
        assertEquals(target.getPassword(), users.selectOneById(target.getId()).getPassword());
    }

    @Test void administratorRoleWithoutUserPermissionCannotResetPasswords() throws Exception {
        var caller = scopedRole("ROLE_ADMIN", "article:update"); var target = user(3L);
        reset(caller, target, reset(PASSWORD, NEW_PASSWORD), 403, 403);
        assertEquals(target.getPassword(), users.selectOneById(target.getId()).getPassword());
    }

    @Test void prefixedAdministrativeRolesCannotBypassTargetHierarchyProtection() throws Exception {
        var admin = scopedRole("ROLE_ADMIN", "user:update");
        var target = scopedRole("ROLE_ADMIN", "article:update");
        reset(admin, target, reset(PASSWORD, NEW_PASSWORD), 403, 403);
        assertEquals(target.getPassword(), users.selectOneById(target.getId()).getPassword());
    }

    @Test void operatorsCannotUseResetToBypassTheirOwnPasswordChangeFlow() throws Exception {
        var root = user(1L);
        reset(root, root, reset(PASSWORD, NEW_PASSWORD), 403, 1010);
        assertEquals(root.getPassword(), users.selectOneById(root.getId()).getPassword());
    }

    @Test void validOperatorProofDoesNotTurnTargetPermissionErrorsIntoPasswordFailures() throws Exception {
        var caller = scopedRole("ROLE_ADMIN", "user:update"); var target = user(1L);
        for (int i = 0; i < 7; i++) reset(caller, target, reset(PASSWORD, NEW_PASSWORD), 403, 403);
        assertEquals(target.getPassword(), users.selectOneById(target.getId()).getPassword());
    }

    @Test void resettingAnotherRootDoesNotChangeRolesOrEnableDisabledAccounts() throws Exception {
        var caller = user(1L); var target = user(1L);
        users.update(User.create().setId(target.getId()).setStatus(1));
        reset(caller, target, reset(PASSWORD, NEW_PASSWORD), 200, 200);
        assertEquals(1, users.selectOneById(target.getId()).getStatus());
        assertTrue(passwords.matches(NEW_PASSWORD, users.selectOneById(target.getId()).getPassword()));
    }

    @Test void incorrectOperatorCredentialsAndMismatchedPasswordsDoNotChangeAnything() throws Exception {
        var root = user(1L); var target = user(3L);
        reset(root, target, reset("incorrect", NEW_PASSWORD), 400, 40022);
        var mismatch = reset(PASSWORD, NEW_PASSWORD); mismatch.setConfirmPassword("different");
        reset(root, target, mismatch, 400, 40021);
        assertEquals(target.getPassword(), users.selectOneById(target.getId()).getPassword());
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "", "中文中文中文中文中文中文中文中文中文中文中文中文中文中文中文中文"})
    void weakOrBcryptOversizedPasswordsAreRejected(String password) throws Exception {
        var root = user(1L); var target = user(3L);
        reset(root, target, reset(PASSWORD, password), 400, 400);
        assertEquals(target.getPassword(), users.selectOneById(target.getId()).getPassword());
    }

    @Test void updateFailureRollsBackAndLeavesTargetSessionsUsable() throws Exception {
        var root = user(1L); var target = user(3L); String old = bearer(target);
        doReturn(0).when(users).updateByQuery(any(User.class), any(QueryWrapper.class));
        reset(root, target, reset(PASSWORD, NEW_PASSWORD), 409, 40920);
        assertEquals(target.getPassword(), users.selectOneById(target.getId()).getPassword());
        mvc.perform(get("/users/" + target.getId()).header("Authorization", old)).andExpect(status().isOk());
    }

    @Test void resettingToTheSamePlaintextStillRevokesOldSessions() throws Exception {
        var root = user(1L); var target = user(3L); String old = bearer(target);
        reset(root, target, reset(PASSWORD, PASSWORD), 200, 200);
        assertNotEquals(target.getPassword(), users.selectOneById(target.getId()).getPassword());
        mvc.perform(get("/users/" + target.getId()).header("Authorization", old)).andExpect(status().isUnauthorized());
    }

    @Test void loginLogsUseThePeerAddressRatherThanForgedForwardingHeaders() throws Exception {
        var caller = user(3L);
        var response = mvc.perform(post("/auth/login").with(request -> { request.setRemoteAddr("198.51.100.77"); return request; })
                        .header("X-Forwarded-For", "1.2.3.4").header("Forwarded", "for=1.2.3.4")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("account", caller.getNid(), "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        sessions.add(json.readTree(response.getResponse().getContentAsString()).path("data").path("token").asText());
        var record = logs.selectOneByCondition(LOGIN_LOG.USER_ID.eq(caller.getId()));
        assertNotNull(record); assertEquals("198.51.100.77", record.getIp());
    }
}
