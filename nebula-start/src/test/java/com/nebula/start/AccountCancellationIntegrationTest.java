package com.nebula.start;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.mybatisflex.core.row.RowKey;
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
@SpringBootTest(properties = "nebula.publication.enabled=false")
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
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired org.springframework.context.ConfigurableApplicationContext context;

    /** 为指定账号创建专用私人数据，公开系列和私人系列同时用于保留边界验证。 */
    private long seedPrivateData(User owner, long articleId, boolean publicVisible) {
        Db.updateBySql("INSERT INTO article_workspace(user_id,document_key,revision,payload) VALUES(?, 'new',1,'{}')", owner.getId());
        Db.updateBySql("INSERT INTO article_reading(user_id,article_id,favorite,later,progress) VALUES(?,?,1,1,5000)", owner.getId(), articleId);
        return series(owner.getId(), articleId, publicVisible);
    }

    /** 系列及关联仅使用事务内文章，不依赖真实账号。 */
    private long series(long owner, long articleId, boolean publicVisible) {
        Row row = Row.ofKey(RowKey.AUTO);
        row.set("user_id", owner); row.set("title", "注销清理回归"); row.set("public_visible", publicVisible ? 1 : 0);
        Db.insert("article_series", row);
        long id = row.getLong("id");
        Db.updateBySql("INSERT INTO article_series_item(series_id,article_id,position) VALUES(?,?,0)", id, articleId);
        return id;
    }

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
        long privateSeries = seedPrivateData(owner, article.getId(), false);
        long publicSeries = series(owner.getId(), article.getId(), true);
        long otherSeries = seedPrivateData(bystander, article.getId(), false);
        var draft = Article.create().setUserId(owner.getId()).setTitle("保留的正式草稿").setContent("草稿正文")
                .setAuthor(owner.getUsername()).setStatus(0).setDeleted(0).setLikeCount(0).setViewCount(0L);
        articles.insert(draft);
        Db.updateBySql("INSERT INTO article_revision(article_id,edit_version,editor_id,title,payload) VALUES(?,0,?,'保留版本','{}')", draft.getId(), owner.getId());
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
        assertNotNull(articles.selectOneById(draft.getId()));
        assertNull(Db.selectOneBySql("SELECT * FROM article_workspace WHERE user_id=?", owner.getId()));
        assertNull(Db.selectOneBySql("SELECT * FROM article_reading WHERE user_id=?", owner.getId()));
        assertNull(Db.selectOneBySql("SELECT * FROM article_series WHERE id=?", privateSeries));
        assertNull(Db.selectOneBySql("SELECT * FROM article_series_item WHERE series_id=?", privateSeries));
        assertNotNull(Db.selectOneBySql("SELECT * FROM article_series WHERE id=?", publicSeries));
        assertNotNull(Db.selectOneBySql("SELECT * FROM article_series_item WHERE series_id=?", publicSeries));
        assertNotNull(Db.selectOneBySql("SELECT * FROM article_workspace WHERE user_id=?", bystander.getId()));
        assertNotNull(Db.selectOneBySql("SELECT * FROM article_reading WHERE user_id=?", bystander.getId()));
        assertNotNull(Db.selectOneBySql("SELECT * FROM article_series WHERE id=?", otherSeries));
        assertNotNull(Db.selectOneBySql("SELECT * FROM article_revision WHERE article_id=?", draft.getId()));
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

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void cleanupFailureRollsBackAccountRolesAndPrivateDataTogether() throws Exception {
        User owner = insertFixture("清理失败回滚账号");
        try {
            userRoles.insert(UserRole.create().setUserId(owner.getId()).setRoleId(3L));
            var article = Article.create().setUserId(owner.getId()).setTitle("回滚验证文章").setContent("正文")
                    .setAuthor(owner.getUsername()).setStatus(1).setDeleted(0).setLikeCount(0).setViewCount(0L);
            articles.insert(article);
            long series = seedPrivateData(owner, article.getId(), false);
            var multicaster = context.getBean("applicationEventMulticaster", org.springframework.context.event.ApplicationEventMulticaster.class);
            org.springframework.context.ApplicationListener<org.springframework.context.PayloadApplicationEvent<?>> failure = event -> {
                if (event.getPayload() instanceof com.nebula.common.security.AccountCancelledEvent cancelled && cancelled.userId() == owner.getId())
                    throw new IllegalStateException("模拟注销监听失败");
            };
            String token = jwt.createToken(owner); sessionTokens.add(token);
            multicaster.addApplicationListener(failure);
            var cancellation = new org.springframework.transaction.support.TransactionTemplate(transactions);
            try {
                assertThrows(IllegalStateException.class, () -> cancellation.executeWithoutResult(transaction -> {
                    try {
                        mvc.perform(delete("/users/self").header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"test-only-password\",\"confirmed\":true}"))
                                .andExpect(status().is5xxServerError());
                    } catch (Exception e) { throw new IllegalStateException(e); }
                    throw new IllegalStateException("回滚失败的注销事务");
                }));
            } finally { multicaster.removeApplicationListener(failure); }
            assertNotNull(users.selectOneById(owner.getId()));
            assertEquals(1L, userRoles.selectCountByCondition(USER_ROLE.USER_ID.eq(owner.getId())));
            assertNotNull(Db.selectOneBySql("SELECT * FROM article_workspace WHERE user_id=?", owner.getId()));
            assertNotNull(Db.selectOneBySql("SELECT * FROM article_reading WHERE user_id=?", owner.getId()));
            assertNotNull(Db.selectOneBySql("SELECT * FROM article_series_item WHERE series_id=?", series));
        } finally {
            // 本例需跨事务核对回滚，只删除本例新建账号及其专用数据。
            new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(transaction -> {
                Db.deleteBySql("DELETE i FROM article_series_item i JOIN article_series s ON s.id=i.series_id WHERE s.user_id=?", owner.getId());
                Db.deleteBySql("DELETE FROM article_series WHERE user_id=?", owner.getId());
                Db.deleteBySql("DELETE FROM article_workspace WHERE user_id=?", owner.getId());
                Db.deleteBySql("DELETE FROM article_reading WHERE user_id=?", owner.getId());
                Db.deleteBySql("DELETE FROM article WHERE user_id=?", owner.getId());
                Db.deleteBySql("DELETE FROM user_role WHERE user_id=?", owner.getId());
                Db.deleteBySql("DELETE FROM user WHERE id=?", owner.getId());
            });
        }
    }
}
