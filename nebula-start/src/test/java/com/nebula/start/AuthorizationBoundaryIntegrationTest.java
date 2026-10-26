package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.nebula.article.entity.Article;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.auth.AuthUserBuilder;
import com.nebula.auth.util.JwtUtil;
import com.nebula.role.dto.CreatePermissionDto;
import com.nebula.role.dto.CreateRoleDto;
import com.nebula.role.dto.SaveRolePermissionsDto;
import com.nebula.role.dto.UpdatePermissionDto;
import com.nebula.role.dto.UpdateRoleDto;
import com.nebula.role.entity.Permission;
import com.nebula.role.entity.Role;
import com.nebula.role.entity.RolePermission;
import com.nebula.role.mapper.PermissionMapper;
import com.nebula.role.mapper.RoleMapper;
import com.nebula.role.mapper.RolePermissionMapper;
import com.nebula.role.service.PermissionService;
import com.nebula.role.service.RoleService;
import com.nebula.user.entity.User;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static com.nebula.role.entity.table.PermissionTableDef.PERMISSION;
import static com.nebula.role.entity.table.RolePermissionTableDef.ROLE_PERMISSION;
import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthorizationBoundaryIntegrationTest extends JwtIntegrationTestSupport {
    private final List<String> sessionTokens = new ArrayList<>();
    @Autowired UserMapper users;
    @Autowired UserRoleMapper userRoles;
    @Autowired RoleMapper roles;
    @Autowired RolePermissionMapper rolePermissions;
    @Autowired PermissionMapper permissions;
    @Autowired ArticleMapper articles;
    @Autowired PermissionService permissionService;
    @Autowired RoleService roleService;
    @Autowired AuthUserBuilder principals;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtUtil jwt;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
        sessionTokens.forEach(jwt::revokeToken);
    }

    @Test void currentSessionIsAvailableWithoutManagementPermissionsAndContainsNoSecrets() throws Exception {
        var ordinary = user(null);
        mvc.perform(get("/auth/session").header("Authorization", bearer(ordinary)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.id").value(ordinary.getId()))
                .andExpect(jsonPath("$.data.roles").isEmpty()).andExpect(jsonPath("$.data.permissions").isEmpty())
                .andExpect(jsonPath("$.data.user.password").doesNotExist()).andExpect(jsonPath("$.data.token").doesNotExist());
    }

    @Test void sessionIdentityCannotBeSelectedThroughAQueryParameter() throws Exception {
        var caller = user(3L); var other = user(1L);
        mvc.perform(get("/auth/session").param("userId", other.getId().toString()).header("Authorization", bearer(caller)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.id").value(caller.getId()));
    }

    @Test void sessionAuthorizationRefreshesPermissionsForAnExistingToken() throws Exception {
        var role = customRole("article:list"); var caller = user(role.getId()); var token = bearer(caller);
        mvc.perform(get("/auth/session").header("Authorization", token)).andExpect(jsonPath("$.data.permissions[0]").value("article:list"));
        rolePermissions.deleteByCondition(ROLE_PERMISSION.ROLE_ID.eq(role.getId()));
        rolePermissions.insert(RolePermission.create().setRoleId(role.getId()).setPermissionId(permission("user:list").getId()));
        mvc.perform(get("/auth/session").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions[0]").value("user:list"));
    }

    @Test void sessionAuthorizationRefreshesRolesAfterDemotion() throws Exception {
        var caller = user(2L); var token = bearer(caller);
        mvc.perform(get("/auth/session").header("Authorization", token)).andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));
        userRoles.deleteByCondition(USER_ROLE.USER_ID.eq(caller.getId()));
        userRoles.insert(UserRole.create().setUserId(caller.getId()).setRoleId(3L));
        mvc.perform(get("/auth/session").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("USER"));
    }

    @Test void currentSessionRejectsAnonymousRevokedAndDisabledSessions() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/auth/session")).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/session").header("Authorization", "Bearer invalid-token")).andExpect(status().isUnauthorized());
        var caller = user(3L); String token = bearer(caller); jwt.revokeToken(token.substring(7));
        mvc.perform(get("/auth/session").header("Authorization", token)).andExpect(status().isUnauthorized());
        var disabled = user(3L); String disabledToken = bearer(disabled);
        users.update(User.create().setId(disabled.getId()).setStatus(1));
        mvc.perform(get("/auth/session").header("Authorization", disabledToken)).andExpect(status().isUnauthorized());
    }

    private User user(Long roleId) {
        var user = User.create().setNid("authorization_" + UUID.randomUUID())
                .setUsername("Authorization fixture").setPassword(passwords.encode("fixture-password"))
                .setStatus(0).setDeleted(0);
        users.insert(user);
        if (roleId != null) userRoles.insert(UserRole.create().setUserId(user.getId()).setRoleId(roleId));
        return user;
    }

    private Role customRole(String... permissionNames) {
        var role = Role.create().setName("AUTH_TEST_" + UUID.randomUUID().toString().replace("-", ""))
                .setDescription("Authorization fixture").setDeleted(0);
        roles.insert(role);
        for (String name : permissionNames) {
            rolePermissions.insert(RolePermission.create().setRoleId(role.getId()).setPermissionId(permission(name).getId()));
        }
        return role;
    }

    private Permission permission(String name) {
        var permission = permissions.selectOneByCondition(PERMISSION.NAME.eq(name));
        assertNotNull(permission, name);
        return permission;
    }

    private String bearer(User user) {
        String token = jwt.createToken(user);
        sessionTokens.add(token);
        return "Bearer " + token;
    }

    private String body(Object value) throws Exception { return json.writeValueAsString(value); }

    private List<Long> assignedPermissions(Long roleId) {
        return rolePermissions.selectListByCondition(ROLE_PERMISSION.ROLE_ID.eq(roleId))
                .stream().map(RolePermission::getPermissionId).sorted().toList();
    }

    @Test
    void businessPermissionsCannotBeUsedToGrantOrMutatePermissions() throws Exception {
        Role role = customRole("user:create", "user:update", "user:delete");
        User caller = user(role.getId());
        var previous = assignedPermissions(role.getId());
        Permission existing = Permission.create();
        existing.setName("audit:" + UUID.randomUUID().toString().replace("-", ""));
        permissions.insert(existing);
        existing = permission(existing.getName());
        String token = bearer(caller);
        mvc.perform(post("/permissions/save").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("roleId", role.getId(), "permissionIds", List.of(permission("article:list").getId())))))
                .andExpect(jsonPath("$.code").value(403));
        mvc.perform(post("/permissions/create").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "audit:created"))))
                .andExpect(jsonPath("$.code").value(403));
        mvc.perform(put("/permissions/update").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("id", existing.getId(), "name", "audit:renamed"))))
                .andExpect(jsonPath("$.code").value(403));
        mvc.perform(delete("/permissions/remove/" + existing.getId()).header("Authorization", token))
                .andExpect(jsonPath("$.code").value(403));
        assertEquals(previous, assignedPermissions(role.getId()));
        assertEquals(existing.getName(), permissions.selectOneByCondition(PERMISSION.ID.eq(existing.getId())).getName());
    }

    @Test
    void serviceLayerRejectsDirectCallsWithoutSuperAdministratorRole() {
        User admin = user(2L);
        var principal = principals.createLoginUser(admin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        assertThrows(AccessDeniedException.class, () -> permissionService.assignPermissionsToRole(
                new SaveRolePermissionsDto().setRoleId(3L).setPermissionIds(List.of())));
        assertThrows(AccessDeniedException.class, () -> permissionService.createPermission(new CreatePermissionDto().setName("audit:created")));
        assertThrows(AccessDeniedException.class, () -> permissionService.updatePermission(new UpdatePermissionDto().setId(2L).setName("audit:renamed")));
        assertThrows(AccessDeniedException.class, () -> permissionService.removePermission(2L));
        assertThrows(AccessDeniedException.class, () -> roleService.createRole(new CreateRoleDto()));
        assertThrows(AccessDeniedException.class, () -> roleService.removeRole(3L));
        assertThrows(AccessDeniedException.class, () -> roleService.updateRole(new UpdateRoleDto()));
    }

    @Test
    void superAdministratorHasAllCatalogPermissionsAndCanUseAdminExpressions() throws Exception {
        User root = user(1L);
        var catalog = permissions.selectListByQuery(QueryWrapper.create()).stream().map(Permission::getName).sorted().toList();
        assertEquals(catalog, permissionService.getPermissionsByUserId(root.getId()).stream().sorted().toList());
        assertEquals(catalog.size(), permissionService.getPermissionByRoleId("1").values().stream().mapToInt(List::size).sum());
        mvc.perform(get("/roles/page").param("current", "1").param("size", "10").header("Authorization", bearer(root)))
                .andExpect(jsonPath("$.code").value(200));
        var article = Article.create().setUserId(root.getId()).setTitle("Authorization fixture")
                .setContent("Fixture content").setStatus(0).setDeleted(0).setViewCount(0L).setLikeCount(0);
        articles.insert(article);
        mvc.perform(put("/articles/status").header("Authorization", bearer(root)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("id", article.getId(), "status", 1))))
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void superAdministratorCanAssignPermissionsToOtherRolesAndClearThem() throws Exception {
        User root = user(1L);
        Role role = customRole("tag:list");
        long read = permission("article:list").getId();
        mvc.perform(post("/permissions/save").header("Authorization", bearer(root)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("roleId", role.getId(), "permissionIds", List.of(read, read)))))
                .andExpect(jsonPath("$.code").value(200));
        assertEquals(List.of(read), assignedPermissions(role.getId()));
        mvc.perform(post("/permissions/save").header("Authorization", bearer(root)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("roleId", role.getId(), "permissionIds", List.of()))))
                .andExpect(jsonPath("$.code").value(200));
        assertTrue(assignedPermissions(role.getId()).isEmpty());
    }

    @Test
    void onlyASuperAdministratorCanPromoteAnAdministrator() throws Exception {
        User root = user(1L);
        User admin = user(2L);
        Role targetRole = customRole("tag:list");
        String oldAdminToken = bearer(admin);
        var promotion = Map.of("id", admin.getId(), "username", admin.getUsername(), "roleIds", List.of(1L));
        mvc.perform(put("/users/edit").header("Authorization", oldAdminToken).contentType(MediaType.APPLICATION_JSON)
                        .content(body(promotion)))
                .andExpect(jsonPath("$.code").value(403));
        mvc.perform(put("/users/edit").header("Authorization", bearer(root)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(promotion)))
                .andExpect(jsonPath("$.code").value(200));
        assertEquals(List.of(1L), userRoles.selectListByCondition(USER_ROLE.USER_ID.eq(admin.getId()))
                .stream().map(UserRole::getRoleId).toList());
        // Existing tokens reload database permissions; the promoted user's next request has the new role.
        mvc.perform(post("/permissions/save").header("Authorization", oldAdminToken).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("roleId", targetRole.getId(), "permissionIds", List.of(permission("article:list").getId())))))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void invalidReferencesCannotEraseExistingRoleOrUserAssignments() throws Exception {
        User root = user(1L);
        User admin = user(2L);
        Role role = customRole("tag:list");
        var previous = assignedPermissions(role.getId());
        mvc.perform(post("/permissions/save").header("Authorization", bearer(root)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("roleId", role.getId(), "permissionIds", List.of(Long.MAX_VALUE)))))
                .andExpect(jsonPath("$.code").value(1102));
        assertEquals(previous, assignedPermissions(role.getId()));
        mvc.perform(post("/permissions/save").header("Authorization", bearer(root)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("roleId", Long.MAX_VALUE, "permissionIds", List.of()))))
                .andExpect(jsonPath("$.code").value(1101));
        mvc.perform(put("/users/edit").header("Authorization", bearer(root)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("id", admin.getId(), "username", "Must not change", "roleIds", List.of(Long.MAX_VALUE)))))
                .andExpect(jsonPath("$.code").value(1105));
        assertEquals(admin.getUsername(), users.selectOneById(admin.getId()).getUsername());
        assertEquals(2L, userRoles.selectListByCondition(USER_ROLE.USER_ID.eq(admin.getId())).get(0).getRoleId());
    }

    @Test
    void implicitRootPermissionsAndBuiltInRoleCodesAreProtected() throws Exception {
        User root = user(1L);
        mvc.perform(post("/permissions/save").header("Authorization", bearer(root)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("roleId", 1L, "permissionIds", List.of()))))
                .andExpect(jsonPath("$.code").value(1104));
        mvc.perform(put("/roles/update").header("Authorization", bearer(root)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("id", 1L, "name", "REPLACED", "description", "fixture"))))
                .andExpect(jsonPath("$.code").value(1103));
        mvc.perform(delete("/roles/remove/1").header("Authorization", bearer(root)))
                .andExpect(jsonPath("$.code").value(1103));
        assertEquals("SUPER_ADMIN", roles.selectOneById(1L).getName());
    }

    @Test
    void administratorsCannotDisableOrDeleteSuperAdministratorAccounts() throws Exception {
        User admin = user(2L);
        User root = user(1L);
        mvc.perform(put("/users/switchStatusById/" + root.getId()).param("status", "1").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.code").value(403));
        mvc.perform(delete("/users/deleteUserById/" + root.getId()).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.code").value(403));
        assertEquals(0, users.selectOneById(root.getId()).getStatus());
    }

    @Test
    void rootCanManageAnotherRootWhileAnotherActiveRootRemains() throws Exception {
        User caller = user(1L);
        User target = user(1L);
        mvc.perform(put("/users/switchStatusById/" + target.getId()).param("status", "1")
                        .header("Authorization", bearer(caller)))
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data").value(true));
        assertEquals(1, users.selectOneById(target.getId()).getStatus());
        mvc.perform(delete("/users/deleteUserById/" + target.getId()).header("Authorization", bearer(caller)))
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data").value(true));
        assertNull(users.selectOneById(target.getId()));
        assertEquals(0, users.selectOneById(caller.getId()).getStatus());
    }

    @Test
    void permissionAssignmentRequiresWellFormedIdentifiers() throws Exception {
        User root = user(1L);
        for (String invalid : List.of("{}", "{\"roleId\":3}", "{\"roleId\":0,\"permissionIds\":[]}",
                "{\"roleId\":3,\"permissionIds\":[null]}", "{\"roleId\":3,\"permissionIds\":[-1]}")) {
            mvc.perform(post("/permissions/save").header("Authorization", bearer(root)).contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(jsonPath("$.code").value(400));
        }
    }
}
