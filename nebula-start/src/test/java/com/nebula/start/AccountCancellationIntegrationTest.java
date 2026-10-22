package com.nebula.start;

import com.mybatisflex.core.query.QueryWrapper;
import com.nebula.article.entity.Article;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.auth.util.JwtUtil;
import com.nebula.comment.entity.Comment;
import com.nebula.comment.mapper.CommentMapper;
import com.nebula.role.mapper.RoleMapper;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import static com.nebula.user.entity.table.UserRoleTableDef.USER_ROLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** All fixtures are inserted through MyBatis-Flex and rolled back after the test. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountCancellationIntegrationTest extends JwtIntegrationTestSupport {
    private final List<String> sessionTokens = new ArrayList<>();
    @Autowired UserMapper users;
    @Autowired UserRoleMapper userRoles;
    @Autowired RoleMapper roles;
    @Autowired ArticleMapper articles;
    @Autowired CommentMapper comments;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtUtil jwt;
    @Autowired MockMvc mvc;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
        sessionTokens.forEach(jwt::revokeToken);
    }

    private User insertFixture(String name) {
        var user = User.create().setNid("cancel_test_" + UUID.randomUUID())
                .setUsername(name).setPassword(passwords.encode("test-only-password"))
                .setStatus(0).setDeleted(0);
        users.insert(user);
        return user;
    }

    @Test
    void cancellationDeletesOnlyTheOwnerAndRejectsItsExistingTokenWhileRetainingContent() throws Exception {
        User owner = insertFixture("Cancellation fixture");
        User bystander = insertFixture("Unaffected fixture");
        assertNotNull(roles.selectOneById(3L), "The project must have the ordinary-user role");
        userRoles.insert(UserRole.create().setUserId(owner.getId()).setRoleId(3L));
        var article = Article.create().setUserId(owner.getId()).setTitle("Cancellation fixture article")
                .setContent("Retained fixture content").setAuthor(owner.getUsername())
                .setStatus(1).setDeleted(0).setLikeCount(0).setViewCount(0L);
        articles.insert(article);
        var comment = Comment.create().setUserId(owner.getId()).setArticleId(article.getId())
                .setContent("Retained fixture comment").setParentId(0L).setStatus(0).setLikeCount(0L);
        comments.insert(comment);
        String existingToken = jwt.createToken(owner);
        sessionTokens.add(existingToken);
        mvc.perform(delete("/users/self").header("Authorization", "Bearer " + existingToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"test-only-password\",\"confirmed\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
        assertNull(users.selectOneById(owner.getId()));
        assertNotNull(users.selectOneById(bystander.getId()));
        assertEquals(0L, userRoles.selectCountByCondition(USER_ROLE.USER_ID.eq(owner.getId())));
        assertNotNull(articles.selectOneById(article.getId()));
        assertNotNull(comments.selectOneById(comment.getId()));
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/profile/" + owner.getId())
                        .header("Authorization", "Bearer " + existingToken))
                .andExpect(status().isUnauthorized());
        String bystanderToken = jwt.createToken(bystander);
        sessionTokens.add(bystanderToken);
        mvc.perform(get("/users/" + owner.getId()).header("Authorization", "Bearer " + bystanderToken))
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/profile/" + owner.getId()).header("Authorization", "Bearer " + bystanderToken))
                .andExpect(jsonPath("$.code").value(40101));
        mvc.perform(get("/users/batch").param("ids", String.valueOf(owner.getId()), String.valueOf(bystander.getId()))
                        .header("Authorization", "Bearer " + bystanderToken))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(bystander.getId()));
    }
}
