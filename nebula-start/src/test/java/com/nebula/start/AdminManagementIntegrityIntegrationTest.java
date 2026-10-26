package com.nebula.start;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.nebula.article.entity.*;
import com.nebula.article.mapper.*;
import com.nebula.auth.util.JwtUtil;
import com.nebula.category.entity.Category;
import com.nebula.category.mapper.CategoryMapper;
import com.nebula.comment.entity.Comment;
import com.nebula.comment.mapper.CommentMapper;
import com.nebula.common.mail.service.MailService;
import com.nebula.role.entity.*;
import com.nebula.role.mapper.*;
import com.nebula.tag.entity.Tag;
import com.nebula.tag.mapper.TagMapper;
import com.nebula.user.entity.User;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.nebula.article.entity.table.ArticleTableDef.ARTICLE;
import static com.nebula.common.constant.redisKey.ArticleRedisKey.*;
import static com.nebula.role.entity.table.PermissionTableDef.PERMISSION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 管理回归使用完整安全链和事务回滚的MyBatis-Flex专用数据。 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminManagementIntegrityIntegrationTest extends JwtIntegrationTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtUtil jwt;
    @Autowired StringRedisTemplate redis;
    @Autowired UserMapper users;
    @Autowired UserRoleMapper userRoles;
    @Autowired ArticleMapper articles;
    @Autowired com.nebula.article.service.ArticleService articleService;
    @Autowired com.nebula.user.service.UserService userService;
    @Autowired com.nebula.role.service.RoleService roleService;
    @Autowired com.nebula.common.redis.RedisService cache;
    @Autowired ArticleCategoryMapper articleCategories;
    @Autowired ArticleTagMapper articleTags;
    @Autowired CategoryMapper categories;
    @Autowired TagMapper tags;
    @Autowired RoleMapper roles;
    @Autowired PermissionMapper permissions;
    @Autowired RolePermissionMapper rolePermissions;
    @Autowired CommentMapper comments;
    @MockitoBean MailService mail;

    private final List<String> tokens = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> articleIds = new ArrayList<>();
    private String marker;
    private User root;

    @BeforeEach void setup() {
        marker = "mf_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        root = user("operator", 1L);
    }

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        tokens.forEach(jwt::revokeToken);
        var keys = new ArrayList<String>();
        for (Long articleId : articleIds) {
            keys.add(ARTICLE_DETAIL + articleId);
            for (Long userId : userIds) keys.add(ARTICLE_VIEW + articleId + ":" + userId);
        }
        if (!keys.isEmpty()) redis.delete(keys);
        verifyNoInteractions(mail);
    }

    @AfterTransaction void verifyRollback() throws Exception {
        var remaining = Db.selectListBySql("SELECT id FROM article WHERE title=?", marker);
        if (!remaining.isEmpty()) {
            // 即使实现意外使用独立事务，失败写入也不能留下本轮专用数据。
            for (var row : remaining) {
                Long id = row.getLong("id");
                Db.deleteBySql("DELETE FROM article_category WHERE article_id=?", id);
                Db.deleteBySql("DELETE FROM article_tag WHERE article_id=?", id);
                Db.deleteBySql("DELETE FROM article WHERE id=? AND title=?", id, marker);
            }
            fail("Article writes unexpectedly survived the rollback");
        }
    }

    @AfterAll static void verifyNoAuditFixturesRemain() throws Exception {
        Map<String, Long> remaining = new LinkedHashMap<>();
        remaining.put("users", Db.selectOneBySql("SELECT COUNT(*) AS remaining FROM user WHERE nid REGEXP '^mf_[0-9a-f]{16}_'").getLong("remaining"));
        remaining.put("articles", Db.selectOneBySql("SELECT COUNT(*) AS remaining FROM article WHERE title REGEXP '^mf_[0-9a-f]{16}$'").getLong("remaining"));
        remaining.put("categories", Db.selectOneBySql("SELECT COUNT(*) AS remaining FROM category WHERE name REGEXP '^mf_[0-9a-f]{16}'").getLong("remaining"));
        remaining.put("tags", Db.selectOneBySql("SELECT COUNT(*) AS remaining FROM tag WHERE name REGEXP '^mf_[0-9a-f]{16}'").getLong("remaining"));
        remaining.put("roles", Db.selectOneBySql("SELECT COUNT(*) AS remaining FROM role WHERE name REGEXP '^mf_[0-9a-f]{16}$' OR description REGEXP '^mf_[0-9a-f]{16}$'").getLong("remaining"));
        remaining.put("comments", Db.selectOneBySql("SELECT COUNT(*) AS remaining FROM comment WHERE content REGEXP '^mf_[0-9a-f]{16}$'").getLong("remaining"));
        assertTrue(remaining.values().stream().allMatch(count -> count == 0));
        System.out.println("ADMIN_REGRESSION fixtures_remaining " + new ObjectMapper().writeValueAsString(remaining));
    }

    private User user(String name, Long... roleIds) {
        User user = User.create().setNid(marker + "_" + name).setUsername(marker + "_" + name)
                .setPassword("isolated-audit-session-binding").setStatus(0).setDeleted(0);
        users.insert(user); userIds.add(user.getId());
        for (Long roleId : roleIds) userRoles.insert(UserRole.create().setUserId(user.getId()).setRoleId(roleId));
        return user;
    }

    private Article article(int status) {
        Article article = Article.create().setUserId(root.getId()).setAuthor(root.getUsername())
                .setTitle(marker).setContent("isolated audit content").setStatus(status)
                .setDeleted(0).setViewCount(0L).setLikeCount(0);
        articles.insert(article); articleIds.add(article.getId()); return article;
    }

    private Category category(String suffix) {
        Category category = Category.create().setName(marker + suffix).setDescription("Audit fixture").setDeleted(0);
        categories.insert(category); return category;
    }

    private Tag tag(String suffix) {
        Tag tag = Tag.create().setName(marker + suffix).setDescription("Audit fixture").setDeleted(0);
        tags.insert(tag); return tag;
    }

    private MvcResult request(User actor, MockHttpServletRequestBuilder request, int http) throws Exception {
        SecurityContextHolder.clearContext();
        if (actor != null) {
            String token = jwt.createToken(actor); tokens.add(token);
            request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request).andExpect(status().is(http)).andReturn();
    }

    private MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, Object value) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(value));
    }

    private JsonNode data(MvcResult response) throws Exception {
        return json.readTree(response.getResponse().getContentAsString()).path("data");
    }



    @Test void uncachedArticleDeletionReturnsSuccessWhenTheDatabaseWriteSucceeds() throws Exception {
        var published = article(1);
        assertFalse(Boolean.TRUE.equals(redis.hasKey(ARTICLE_DETAIL + published.getId())));
        var response = request(root, delete("/articles/" + published.getId()), 200);
        assertTrue(data(response).asBoolean());
        assertEquals(1, Db.selectOneBySql("SELECT deleted FROM article WHERE id=?", published.getId()).getInt("deleted"));
    }

    @Test void userPaginationPreservesRequestedPageSizeAndTotalMetadata() throws Exception {
        user("target1", 3L); user("target2", 3L); user("target3", 3L);
        JsonNode page = data(request(root, get("/users").param("username", marker + "_target")
                .param("current", "2").param("size", "1"), 200));
        assertEquals(1, page.path("records").size());
        assertEquals(3, page.path("totalRow").asLong());
        assertEquals(2, page.path("pageNumber").asInt());
        assertEquals(1, page.path("pageSize").asInt());
    }

    @Test void aNonexistentRoleFilterReturnsAnEmptyPageWithCorrectMetadata() throws Exception {
        var target = user("role_target", 3L);
        JsonNode page = data(request(root, get("/users").param("username", target.getUsername())
                .param("role", marker + "_NONEXISTENT"), 200));
        assertEquals(0, page.path("records").size());
        assertEquals(0, page.path("totalRow").asLong());
        assertEquals(1, page.path("pageNumber").asInt());
    }

    @Test void aMultiRoleUserAppearsOnlyOnceWhenFilteringRoles() throws Exception {
        var target = user("multi_role_target", 2L, 3L);
        JsonNode page = data(request(root, get("/users").param("username", target.getUsername()).param("role", "USER"), 200));
        assertEquals(1, page.path("records").size());
        assertEquals(1, page.path("totalRow").asLong());
        assertEquals(target.getId(), page.path("records").get(0).path("id").asLong());
    }

    @Test void anUnknownUserStatusIsRejectedWithoutChangingTheUser() throws Exception {
        var target = user("status_target", 3L);
        request(root, put("/users/switchStatusById/" + target.getId()).param("status", "99"), 400);
        assertEquals(0, users.selectOneById(target.getId()).getStatus());
    }

    @Test void roleDeletionRemovesBothUserAndPermissionBindings() throws Exception {
        var role = Role.create().setName(marker).setDescription("Audit fixture").setDeleted(0); roles.insert(role);
        var permission = permissions.selectOneByCondition(PERMISSION.NAME.eq("article:list")); assertNotNull(permission);
        rolePermissions.insert(RolePermission.create().setRoleId(role.getId()).setPermissionId(permission.getId()));
        user("custom_role_target", role.getId());
        assertTrue(data(request(root, delete("/roles/remove/" + role.getId()), 200)).asBoolean());
        assertNull(roles.selectOneById(role.getId()));
        assertTrue(Db.selectListBySql("SELECT id FROM role_permission WHERE role_id=?", role.getId()).isEmpty());
        assertTrue(Db.selectListBySql("SELECT user_id FROM user_role WHERE role_id=?", role.getId()).isEmpty());
    }

    @Test void aCategoryUsedByAnActiveArticleCannotBeDeleted() throws Exception {
        var category = category("category"); var article = article(1);
        articleCategories.insert(ArticleCategory.create().setArticleId(article.getId()).setCategoryId(category.getId()));
        request(root, delete("/categories/delete/" + category.getId()), 409);
        assertNotNull(categories.selectOneById(category.getId()));
        assertEquals(1, Db.selectListBySql("SELECT article_id FROM article_category WHERE article_id=?", article.getId()).size());
        JsonNode details = data(request(root, get("/articles/" + article.getId()), 200));
        assertEquals(category.getId(), details.path("categoryId").asLong());
    }

    @Test void aTagUsedByAnActiveArticleCannotBeDeleted() throws Exception {
        var tag = tag("tag"); var article = article(1);
        articleTags.insert(ArticleTag.create().setArticleId(article.getId()).setTagId(tag.getId()));
        request(root, delete("/tags/delete/" + tag.getId()), 409);
        assertNotNull(tags.selectOneById(tag.getId()));
        JsonNode details = data(request(root, get("/articles/" + article.getId()), 200));
        assertEquals(tag.getId(), details.path("tagIds").get(0).asLong());
    }

    @Test void articleCreationRejectsNonexistentCategoryAndTagIdsBeforeSaving() throws Exception {
        request(root, body(post("/articles/create"), Map.of("title", marker, "content", "new draft",
                "categoryId", Long.MAX_VALUE, "tagIds", List.of(Long.MAX_VALUE))), 400);
        var validCategory = category("valid");
        request(root, body(post("/articles/create"), Map.of("title", marker, "content", "new draft",
                "categoryId", validCategory.getId(), "tagIds", List.of(Long.MAX_VALUE))), 400);
        var article = articles.selectOneByQuery(QueryWrapper.create().where(ARTICLE.TITLE.eq(marker)));
        assertNull(article);
    }

    @Test void publishedTagFilteringCountsEachArticleOnceAcrossPages() throws Exception {
        var article = article(1); var first = tag("first"); var second = tag("second");
        articleTags.insert(ArticleTag.create().setArticleId(article.getId()).setTagId(first.getId()));
        articleTags.insert(ArticleTag.create().setArticleId(article.getId()).setTagId(second.getId()));
        String ids = first.getId() + "," + second.getId();
        JsonNode page1 = data(request(root, get("/articles/published").param("title", marker).param("tagIds", ids).param("size", "1").param("current", "1"), 200));
        JsonNode page2 = data(request(root, get("/articles/published").param("title", marker).param("tagIds", ids).param("size", "1").param("current", "2"), 200));
        assertEquals(1, page1.path("totalRow").asLong());
        assertEquals(article.getId(), page1.path("records").get(0).path("id").asLong());
        assertEquals(0, page2.path("records").size());
    }

    @Test void managementArticleListAppliesCategoryAndTagFiltersTogether() throws Exception {
        var firstCategory = category("first"); var secondCategory = category("second");
        var firstTag = tag("first"); var secondTag = tag("second");
        var first = article(1); var second = article(1);
        articleCategories.insert(ArticleCategory.create().setArticleId(first.getId()).setCategoryId(firstCategory.getId()));
        articleCategories.insert(ArticleCategory.create().setArticleId(second.getId()).setCategoryId(secondCategory.getId()));
        articleTags.insert(ArticleTag.create().setArticleId(first.getId()).setTagId(firstTag.getId()));
        articleTags.insert(ArticleTag.create().setArticleId(second.getId()).setTagId(secondTag.getId()));
        JsonNode page = data(request(root, get("/articles").param("title", marker)
                .param("categoryIds", firstCategory.getId().toString()).param("tagIds", firstTag.getId().toString()), 200));
        assertEquals(1, page.path("records").size());
        assertEquals(first.getId(), page.path("records").get(0).path("id").asLong());
    }

    private User moderator() {
        var role = Role.create().setName("ROLE_ADMIN").setDescription(marker).setDeleted(0); roles.insert(role);
        var permission = permissions.selectOneByCondition(PERMISSION.NAME.eq("comment:update")); assertNotNull(permission);
        rolePermissions.insert(RolePermission.create().setRoleId(role.getId()).setPermissionId(permission.getId()));
        return user("comment_moderator", role.getId());
    }

    @Test void aCommentOnlyModeratorCannotOverrideTheirLikeIdentity() throws Exception {
        User actor = moderator(); var victim = user("victim", 3L); var article = article(1);
        var comment = Comment.create().setArticleId(article.getId()).setUserId(root.getId()).setContent(marker).setStatus(0).setLikeCount(0L); comments.insert(comment);
        assertTrue(data(request(actor, body(put("/comments/like"), Map.of("commentId", comment.getId(), "userId", victim.getId())), 200)).asBoolean());
        assertEquals(actor.getId(), Db.selectOneBySql("SELECT user_id FROM comment_like WHERE comment_id=?", comment.getId()).getLong("user_id"));
    }

    @Test void anInvalidLegacyUserIdIsIgnoredInFavorOfTheCurrentActor() throws Exception {
        User actor = moderator(); var article = article(1);
        var comment = Comment.create().setArticleId(article.getId()).setUserId(root.getId()).setContent(marker).setStatus(0).setLikeCount(0L); comments.insert(comment);
        assertTrue(data(request(actor, body(put("/comments/like"), Map.of("commentId", comment.getId(), "userId", Long.MAX_VALUE)), 200)).asBoolean());
        assertEquals(actor.getId(), Db.selectOneBySql("SELECT user_id FROM comment_like WHERE comment_id=?", comment.getId()).getLong("user_id"));
    }

    @Test void userAndRolePaginationRejectInvalidLimitsAndPageNumbers() throws Exception {
        for (String path : List.of("/users", "/roles/page")) {
            request(root, get(path).param("size", "101"), 400);
            request(root, get(path).param("size", "0"), 400);
            request(root, get(path).param("size", "-1"), 400);
            request(root, get(path).param("current", "0"), 400);
        }
    }

    @Test void aMissingCategoryIsRejectedAsBadInputBeforeDatabaseWrites() throws Exception {
        MvcResult response = request(root, body(post("/articles/create"), Map.of("title", marker, "content", "new draft")), 400);
        assertNull(articles.selectOneByQuery(QueryWrapper.create().where(ARTICLE.TITLE.eq(marker))));
    }

    @Test void ordinaryUsersCannotUseAdministrativeMutations() throws Exception {
        var ordinary = user("ordinary_control", 3L);
        var published = article(1);
        request(ordinary, delete("/articles/" + published.getId()), 403);
        request(ordinary, body(post("/categories/create"), Map.of("name", marker)), 403);
        request(ordinary, body(post("/roles/create"), Map.of("name", marker)), 403);
        assertEquals(0, Db.selectOneBySql("SELECT deleted FROM article WHERE id=?", published.getId()).getInt("deleted"));
    }

    @Test void anonymousRequestsCannotReachAdministrativeEndpoints() throws Exception {
        var published = article(1);
        request(null, get("/users"), 401);
        request(null, get("/roles/page"), 401);
        request(null, delete("/articles/" + published.getId()), 401);
    }

    @Test void commentLikesNeedOnlyACommentIdAndToggleOnlyTheCurrentUsersRecord() throws Exception {
        var actor = user("liker", 3L); var article = article(1);
        var comment = Comment.create().setArticleId(article.getId()).setUserId(root.getId()).setContent(marker).setStatus(0).setLikeCount(9L);
        comments.insert(comment);
        assertTrue(data(request(actor, body(put("/comments/like"), Map.of("commentId", comment.getId())), 200)).asBoolean());
        assertEquals(1L, Db.selectOneBySql("SELECT like_count FROM comment WHERE id=?", comment.getId()).getLong("like_count"));
        assertTrue(data(request(root, body(put("/comments/like"), Map.of("commentId", comment.getId())), 200)).asBoolean());
        assertFalse(data(request(actor, body(put("/comments/like"), Map.of("commentId", comment.getId())), 200)).asBoolean());
        assertEquals(root.getId(), Db.selectOneBySql("SELECT user_id FROM comment_like WHERE comment_id=?", comment.getId()).getLong("user_id"));
        assertEquals(1L, Db.selectOneBySql("SELECT like_count FROM comment WHERE id=?", comment.getId()).getLong("like_count"));
    }

    @Test void anActorWithoutCommentPermissionCannotLike() throws Exception {
        var actor = user("no_permissions"); var article = article(1);
        var comment = Comment.create().setArticleId(article.getId()).setUserId(root.getId()).setContent(marker).setStatus(0).setLikeCount(0L);
        comments.insert(comment);
        request(actor, body(put("/comments/like"), Map.of("commentId", comment.getId())), 403);
        assertTrue(Db.selectListBySql("SELECT id FROM comment_like WHERE comment_id=?", comment.getId()).isEmpty());
    }

    @Test void emptyUserPagesRetainTheRequestedMetadataAndCount() throws Exception {
        user("empty_target", 3L);
        var page = data(request(root, get("/users").param("username", marker + "_empty_target")
                .param("current", "3").param("size", "2"), 200));
        assertEquals(0, page.path("records").size());
        assertEquals(3, page.path("pageNumber").asInt());
        assertEquals(2, page.path("pageSize").asInt());
        assertEquals(1, page.path("totalRow").asLong());
    }

    @Test void roleFiltersAcceptAnExactIdAndDescriptionAndIgnoreDeletedRoles() throws Exception {
        var target = user("role_search", 3L);
        for (String filter : List.of("3", roles.selectOneById(3L).getDescription())) {
            var page = data(request(root, get("/users").param("username", target.getUsername()).param("role", filter), 200));
            assertEquals(1, page.path("totalRow").asLong());
            assertEquals(target.getId(), page.path("records").get(0).path("id").asLong());
        }
        var role = Role.create().setName(marker).setDescription(marker).setDeleted(0); roles.insert(role);
        userRoles.insert(UserRole.create().setUserId(target.getId()).setRoleId(role.getId()));
        roles.deleteById(role.getId());
        var page = data(request(root, get("/users").param("username", target.getUsername()).param("role", role.getId().toString()), 200));
        assertEquals(0, page.path("totalRow").asLong());
    }

    @Test void validCatalogBindingsAreSavedOnceAndDeletedTargetsAreRejected() throws Exception {
        var category = category("create"); var tag = tag("create");
        assertTrue(data(request(root, body(post("/articles/create"), Map.of("title", marker, "content", "new draft",
                "categoryId", category.getId(), "tagIds", List.of(tag.getId(), tag.getId()))), 200)).asBoolean());
        var saved = articles.selectOneByQuery(QueryWrapper.create().where(ARTICLE.TITLE.eq(marker))); articleIds.add(saved.getId());
        assertEquals(0, saved.getStatus());
        assertEquals(1, Db.selectListBySql("SELECT tag_id FROM article_tag WHERE article_id=?", saved.getId()).size());
        assertEquals(category.getId(), Db.selectOneBySql("SELECT category_id FROM article_category WHERE article_id=?", saved.getId()).getLong("category_id"));
        tags.deleteById(tag.getId());
        request(root, body(post("/articles/create"), Map.of("title", marker + "_bad", "content", "new draft",
                "categoryId", category.getId(), "tagIds", List.of(tag.getId()))), 400);
        categories.deleteById(category.getId());
        request(root, body(post("/articles/create"), Map.of("title", marker + "_bad", "content", "new draft",
                "categoryId", category.getId())), 400);
    }

    @Test void rejectedArticleUpdatesPreserveContentAndBindings() throws Exception {
        var category = category("update"); var tag = tag("update"); var article = article(1);
        articleCategories.insert(ArticleCategory.create().setArticleId(article.getId()).setCategoryId(category.getId()));
        articleTags.insert(ArticleTag.create().setArticleId(article.getId()).setTagId(tag.getId()));
        request(root, body(put("/articles/edit"), Map.of("id", article.getId(), "title", "must not persist", "content", "bad",
                "categoryId", category.getId(), "tagIds", List.of(Long.MAX_VALUE))), 400);
        assertEquals(marker, Db.selectOneBySql("SELECT title FROM article WHERE id=?", article.getId()).getString("title"));
        assertEquals(tag.getId(), Db.selectOneBySql("SELECT tag_id FROM article_tag WHERE article_id=?", article.getId()).getLong("tag_id"));
    }

    @Test void catalogsOnlyUsedByDeletedArticlesCanBeDeletedAndUnbound() throws Exception {
        var category = category("unused"); var tag = tag("unused"); var article = article(1);
        articleCategories.insert(ArticleCategory.create().setArticleId(article.getId()).setCategoryId(category.getId()));
        articleTags.insert(ArticleTag.create().setArticleId(article.getId()).setTagId(tag.getId()));
        articles.deleteById(article.getId());
        assertTrue(data(request(root, delete("/categories/delete/" + category.getId()), 200)).asBoolean());
        assertTrue(data(request(root, delete("/tags/delete/" + tag.getId()), 200)).asBoolean());
        assertTrue(Db.selectListBySql("SELECT article_id FROM article_category WHERE category_id=?", category.getId()).isEmpty());
        assertTrue(Db.selectListBySql("SELECT article_id FROM article_tag WHERE tag_id=?", tag.getId()).isEmpty());
    }

    @Test void legacyInvalidCatalogIdsAreRemovedFromCachedDetails() throws Exception {
        var category = category("legacy"); var tag = tag("legacy"); var article = article(1);
        articleCategories.insert(ArticleCategory.create().setArticleId(article.getId()).setCategoryId(category.getId()));
        articleTags.insert(ArticleTag.create().setArticleId(article.getId()).setTagId(tag.getId()));
        cache.set(ARTICLE_DETAIL + article.getId(), com.nebula.article.vo.ArticleVO.create().setId(article.getId())
                .setTitle(marker).setContent(article.getContent()).setCategoryId(category.getId()).setTagIds(List.of(tag.getId())));
        assertTrue(Boolean.TRUE.equals(cache.hasKey(ARTICLE_DETAIL + article.getId())));
        com.nebula.article.vo.ArticleVO cached = cache.get(ARTICLE_DETAIL + article.getId());
        assertNotNull(cached);
        assertEquals(List.of(tag.getId()), cached.getTagIds());
        categories.deleteById(category.getId()); tags.deleteById(tag.getId());
        var details = data(request(root, get("/articles/" + article.getId()), 200));
        assertTrue(details.path("categoryId").isNull());
        assertEquals(0, details.path("tagIds").size());
    }

    @Test void legacyArticleSnapshotsRoundTripWithoutReplacingFreshDetails() throws Exception {
        var article = article(1);
        var tag = tag("cached");
        articleTags.insert(ArticleTag.create().setArticleId(article.getId()).setTagId(tag.getId()));
        var first = data(request(root, get("/articles/" + article.getId()), 200));
        // 主动构造旧快照验证Redis序列化，详情读取不再负责写入正文缓存。
        var snapshot = json.treeToValue(first, com.nebula.article.vo.ArticleVO.class).setContent("过期正文");
        cache.set(ARTICLE_DETAIL + article.getId(), snapshot);
        assertTrue(Boolean.TRUE.equals(cache.hasKey(ARTICLE_DETAIL + article.getId())));
        com.nebula.article.vo.ArticleVO cached = cache.get(ARTICLE_DETAIL + article.getId());
        assertNotNull(cached);
        assertEquals(articles.selectOneById(article.getId()).getCreateTime(), cached.getCreateTime());
        assertEquals(List.of(tag.getId()), cached.getTagIds());
        assertEquals(snapshot.getContent(), cached.getContent());
        var hit = data(request(root, get("/articles/" + article.getId()), 200));
        assertEquals(first.path("createTime"), hit.path("createTime"));
        assertEquals(first.path("tagIds"), hit.path("tagIds"));
        assertEquals(first.path("content"), hit.path("content"));
        assertNotEquals(cached.getContent(), hit.path("content").asText());
    }

    @Test void emptyCatalogFiltersIncludeLegacyUnclassifiedArticlesAndSortingIsStable() {
        var first = article(1); var second = article(1);
        var dto = com.nebula.article.dto.ArticlePageDTO.create().setTitle(marker).setCategoryIds(List.of()).setTagIds(List.of()).setSize(1);
        var page1 = articleService.pagePublishedArticles(dto);
        var page2 = articleService.pagePublishedArticles(dto.setCurrent(2));
        assertEquals(2, page1.getTotalRow());
        assertEquals(second.getId(), page1.getRecords().get(0).getId());
        assertEquals(first.getId(), page2.getRecords().get(0).getId());
    }

    @Test void validUserStatusesStillWorkAndServicePaginationAlsoRejectsBadRanges() throws Exception {
        var target = user("valid_status", 3L);
        for (int value : List.of(1, 2, 0)) {
            assertTrue(data(request(root, put("/users/switchStatusById/" + target.getId()).param("status", String.valueOf(value)), 200)).asBoolean());
            assertEquals(value, Db.selectOneBySql("SELECT status FROM user WHERE id=?", target.getId()).getInt("status"));
        }
        assertThrows(com.nebula.common.exception.BusinessException.class, () -> userService.pageUsers(null, null, 1, -1));
        var dto = new com.nebula.role.dto.RolePageDto(); dto.setSize(101);
        assertThrows(com.nebula.common.exception.BusinessException.class, () -> roleService.pageRoles(dto));
    }

    private Comment threadComment(Article article, User user, Long parentId, String suffix) {
        var comment = Comment.create().setArticleId(article.getId()).setUserId(user.getId()).setParentId(parentId)
                .setContent(marker + suffix).setStatus(0).setLikeCount(0L);
        comments.insertSelective(comment); return comment;
    }

    @Test void threadsAndNestedRepliesArePagedSeparatelyWithReplyTargets() throws Exception {
        var article = article(1); var other = user("responder", 3L);
        var rootComment = threadComment(article, root, 0L, " root");
        var first = threadComment(article, other, rootComment.getId(), " first");
        var nested = threadComment(article, root, first.getId(), " nested");
        var roots = data(request(other, get("/comments/threads").param("articleId", article.getId().toString()), 200));
        assertEquals(1, roots.path("totalRow").asLong());
        assertEquals(2, roots.path("records").get(0).path("replyCount").asLong());
        var replyPage = data(request(other, get("/comments/" + rootComment.getId() + "/replies").param("size", "1").param("currentPage", "2"), 200));
        assertEquals(2, replyPage.path("totalRow").asLong());
        var reply = replyPage.path("records").get(0);
        assertEquals(nested.getId(), reply.path("id").asLong());
        assertEquals(first.getId(), reply.path("parentId").asLong());
        assertEquals(other.getUsername(), reply.path("replyToUsername").asText());
        assertEquals(rootComment.getId(), reply.path("rootId").asLong());
        assertFalse(reply.path("createTime").asText().isBlank());
    }

    @Test void rootPaginationDoesNotCountRepliesAsAdditionalThreads() throws Exception {
        var article = article(1); var first = threadComment(article, root, 0L, " first");
        var second = threadComment(article, root, 0L, " second");
        threadComment(article, root, first.getId(), " reply");
        var page = data(request(root, get("/comments/threads").param("articleId", article.getId().toString()).param("size", "1"), 200));
        assertEquals(2, page.path("totalRow").asLong()); assertEquals(second.getId(), page.path("records").get(0).path("id").asLong());
        var next = data(request(root, get("/comments/threads").param("articleId", article.getId().toString()).param("size", "1").param("currentPage", "2"), 200));
        assertEquals(first.getId(), next.path("records").get(0).path("id").asLong());
        assertEquals(1, next.path("records").get(0).path("replyCount").asLong());
    }

    @Test void legacyNullParentsAreStillVisibleAsRoots() throws Exception {
        var article = article(1); var legacy = threadComment(article, root, null, " legacy");
        Db.updateBySql("UPDATE comment SET parent_id=NULL WHERE id=?", legacy.getId());
        var page = data(request(root, get("/comments/threads").param("articleId", article.getId().toString()), 200));
        assertEquals(1, page.path("totalRow").asLong());
        request(root, body(put("/comments/release"), Map.of("articleId", article.getId(), "parentId", legacy.getId(), "content", marker + " new")), 200);
    }

    @Test void repliesUseTheCurrentUserAndPreserveTheActualParent() throws Exception {
        var article = article(1); var actor = user("reply_actor", 3L); var parent = threadComment(article, root, 0L, " root");
        request(actor, body(put("/comments/release"), Map.of("articleId", article.getId(), "parentId", parent.getId(),
                "content", marker + " posted", "userId", root.getId())), 200);
        var row = Db.selectOneBySql("SELECT user_id,parent_id,like_count FROM comment WHERE content=?", marker + " posted");
        assertEquals(actor.getId(), row.getLong("user_id")); assertEquals(parent.getId(), row.getLong("parent_id"));
        assertEquals(0L, row.getLong("like_count"));
    }

    @Test void repliesCannotReferenceAnotherArticle() throws Exception {
        var first = article(1); var second = article(1); var parent = threadComment(first, root, 0L, " root");
        request(root, body(put("/comments/release"), Map.of("articleId", second.getId(), "parentId", parent.getId(), "content", marker + " rejected")), 400);
        assertTrue(Db.selectListBySql("SELECT id FROM comment WHERE article_id=?", second.getId()).isEmpty());
    }

    @Test void missingAndDeletedParentsCannotReceiveReplies() throws Exception {
        var article = article(1); var parent = threadComment(article, root, 0L, " deleted"); comments.deleteById(parent.getId());
        for (Long id : List.of(parent.getId(), Long.MAX_VALUE)) {
            request(root, body(put("/comments/release"), Map.of("articleId", article.getId(), "parentId", id, "content", marker + " rejected")), 404);
        }
    }

    @Test void threadEndpointsAndRepliesRespectPrivateArticleVisibility() throws Exception {
        var article = article(0); var reader = user("private_reader", 3L); var parent = threadComment(article, root, 0L, " private");
        request(reader, get("/comments/threads").param("articleId", article.getId().toString()), 404);
        request(reader, get("/comments/" + parent.getId() + "/replies"), 404);
        request(reader, body(put("/comments/release"), Map.of("articleId", article.getId(), "parentId", parent.getId(), "content", marker + " rejected")), 404);
        request(null, get("/comments/" + parent.getId() + "/replies"), 401);
    }

    @Test void replyInputAndPaginationAreValidated() throws Exception {
        var article = article(1); var parent = threadComment(article, root, 0L, " root");
        for (String content : List.of(" ", "x".repeat(2001))) request(root, body(put("/comments/release"),
                Map.of("articleId", article.getId(), "parentId", parent.getId(), "content", content)), 400);
        request(root, body(put("/comments/release"), Map.of("articleId", article.getId(), "parentId", -1, "content", "valid")), 400);
        request(root, get("/comments/threads").param("articleId", article.getId().toString()).param("size", "101"), 400);
        request(root, get("/comments/" + parent.getId() + "/replies").param("currentPage", "0"), 400);
    }

    @Test void deletingAReplyRemovesOnlyItsBranchAndItsLikes() throws Exception {
        var article = article(1); var other = user("branch_author", 3L); var parent = threadComment(article, root, 0L, " root");
        var branch = threadComment(article, other, parent.getId(), " branch");
        var nested = threadComment(article, root, branch.getId(), " nested");
        var sibling = threadComment(article, root, parent.getId(), " sibling");
        request(root, body(put("/comments/like"), Map.of("commentId", nested.getId())), 200);
        request(other, delete("/comments/delete/" + branch.getId()), 200);
        assertEquals(2, Db.selectOneBySql("SELECT COUNT(*) AS total FROM comment WHERE id IN (?,?) AND status=1", branch.getId(), nested.getId()).getInt("total"));
        assertTrue(Db.selectListBySql("SELECT id FROM comment_like WHERE comment_id=?", nested.getId()).isEmpty());
        var replies = data(request(root, get("/comments/" + parent.getId() + "/replies"), 200));
        assertEquals(1, replies.path("totalRow").asLong()); assertEquals(sibling.getId(), replies.path("records").get(0).path("id").asLong());
        assertNotNull(comments.selectOneById(parent.getId()));
    }

    @Test void deletingARootRemovesAllRepliesAndStopsFurtherReplying() throws Exception {
        var article = article(1); var parent = threadComment(article, root, 0L, " root");
        var first = threadComment(article, root, parent.getId(), " first"); threadComment(article, root, first.getId(), " nested");
        request(root, delete("/comments/delete/" + parent.getId()), 200);
        assertEquals(0, Db.selectOneBySql("SELECT COUNT(*) AS total FROM comment WHERE article_id=? AND status=0", article.getId()).getInt("total"));
        request(root, get("/comments/" + parent.getId() + "/replies"), 404);
        request(root, body(put("/comments/release"), Map.of("articleId", article.getId(), "parentId", first.getId(), "content", marker + " rejected")), 404);
    }

    @Test void anotherAuthorCannotDeleteAThreadTheyOnlyRepliedTo() throws Exception {
        var article = article(1); var other = user("not_owner", 3L); var parent = threadComment(article, root, 0L, " root");
        threadComment(article, other, parent.getId(), " reply");
        request(other, delete("/comments/delete/" + parent.getId()), 403);
        assertNotNull(comments.selectOneById(parent.getId()));
    }

    @Test void deletedAuthorsAndReplyTargetsDoNotExposeTheirOldNamesOrAvatars() throws Exception {
        var article = article(1); var other = user("deleted_commenter", 3L); var parent = threadComment(article, other, 0L, " root");
        threadComment(article, root, parent.getId(), " reply"); users.deleteById(other.getId());
        var roots = data(request(root, get("/comments/threads").param("articleId", article.getId().toString()), 200));
        assertTrue(roots.path("records").get(0).path("username").isNull());
        var replies = data(request(root, get("/comments/" + parent.getId() + "/replies"), 200));
        assertTrue(replies.path("records").get(0).path("replyToUsername").isNull());
    }

    @Test void replyListsIncludeTheCurrentUsersLikeAndTheStoredCount() throws Exception {
        var article = article(1); var parent = threadComment(article, root, 0L, " root");
        var reply = threadComment(article, root, parent.getId(), " reply");
        request(root, body(put("/comments/like"), Map.of("commentId", reply.getId())), 200);
        var page = data(request(root, get("/comments/" + parent.getId() + "/replies"), 200));
        assertTrue(page.path("records").get(0).path("liked").asBoolean());
        assertEquals(1, page.path("records").get(0).path("likeCount").asLong());
    }

    @Test void repliesEndpointRequiresARootAndRejectsBrokenHistoricalAncestry() throws Exception {
        var article = article(1); var parent = threadComment(article, root, 0L, " root");
        var reply = threadComment(article, root, parent.getId(), " reply");
        request(root, get("/comments/" + reply.getId() + "/replies"), 400);
        Db.updateBySql("UPDATE comment SET parent_id=? WHERE id=?", Long.MAX_VALUE, reply.getId());
        request(root, body(put("/comments/release"), Map.of("articleId", article.getId(), "parentId", reply.getId(), "content", "broken")), 404);
    }

    private User writer(String suffix, String... permissionNames) {
        var role = Role.create().setName(marker + "_" + suffix).setDescription(marker).setDeleted(0);
        roles.insert(role);
        for (String name : permissionNames) {
            var permission = permissions.selectOneByCondition(PERMISSION.NAME.eq(name));
            assertNotNull(permission);
            rolePermissions.insert(RolePermission.create().setRoleId(role.getId()).setPermissionId(permission.getId()));
        }
        return user(suffix, role.getId());
    }

    private User writer() {
        return writer("writer", "article:list", "article:create", "article:update", "article:delete");
    }

    @Test void authoringCreateReturnsAnIdAndIgnoresForgedPublicationAndOwnership() throws Exception {
        var writer = writer(); var category = category("authoring"); var tag = tag("authoring");
        var response = data(request(writer, body(post("/articles/drafts"), Map.of(
                "title", marker, "content", "preview draft", "categoryId", category.getId(),
                "tagIds", List.of(tag.getId()), "status", 1, "userId", root.getId(), "author", "forged")), 200));
        assertTrue(response.isIntegralNumber()); long id = response.asLong(); articleIds.add(id);
        var stored = articles.selectOneById(id);
        assertEquals(0, stored.getStatus()); assertEquals(writer.getId(), stored.getUserId());
        assertEquals(writer.getUsername(), stored.getAuthor());
        var detail = data(request(writer, get("/articles/" + id + "/edit"), 200));
        assertEquals(category.getId(), detail.path("categoryId").asLong());
        assertEquals(tag.getId(), detail.path("tagIds").get(0).asLong());
        request(writer, body(put("/articles/edit"), editPayload(stored, category)), 200);
        assertEquals(1, articles.selectCountByQuery(QueryWrapper.create().where(ARTICLE.USER_ID.eq(writer.getId()))));
    }

    @ParameterizedTest @ValueSource(ints = {0, 1})
    void editorReadsDoNotIncreaseViewsOrCreatePublicCache(int articleStatus) throws Exception {
        var owner = writer("edit_only", "article:update"); var article = ownedDraft(owner);
        articles.update(article.setStatus(articleStatus).setViewCount(7L));
        var detail = data(request(owner, get("/articles/" + article.getId() + "/edit"), 200));
        assertEquals(7, detail.path("viewCount").asLong());
        assertEquals(7L, articles.selectOneById(article.getId()).getViewCount());
        assertFalse(Boolean.TRUE.equals(redis.hasKey(ARTICLE_DETAIL + article.getId())));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(ARTICLE_VIEW + article.getId() + ":" + owner.getId())));
    }

    @Test void authoringReadsRejectAnonymousReadersNonOwnersAndDeletedArticles() throws Exception {
        var owner = writer(); var draft = ownedDraft(owner);
        var other = writer("other_editor", "article:update");
        var reader = writer("read_only_editor", "article:list");
        request(null, get("/articles/" + draft.getId() + "/edit"), 401);
        request(reader, get("/articles/" + draft.getId() + "/edit"), 403);
        request(other, get("/articles/" + draft.getId() + "/edit"), 403);
        articles.update(draft.setStatus(1));
        request(other, get("/articles/" + draft.getId() + "/edit"), 403);
        articles.deleteById(draft.getId());
        request(owner, get("/articles/" + draft.getId() + "/edit"), 404);
        request(owner, get("/articles/0/edit"), 400);
    }

    @ParameterizedTest @ValueSource(longs = {1L, 2L})
    void administratorsCanPreviewOtherAuthorsDraftsWithoutPublishing(long roleId) throws Exception {
        var owner = writer(); var draft = ownedDraft(owner); var manager = user("preview_manager", roleId);
        assertEquals(draft.getId(), data(request(manager, get("/articles/" + draft.getId() + "/edit"), 200)).path("id").asLong());
        assertEquals(0, articles.selectOneById(draft.getId()).getStatus());
    }

    @Test void authoringCreateRequiresPermissionAndValidInputs() throws Exception {
        var reader = writer("read_only_create", "article:list"); var category = category("new_editor");
        var payload = Map.of("title", marker, "content", "draft", "categoryId", category.getId());
        request(null, body(post("/articles/drafts"), payload), 401);
        request(reader, body(post("/articles/drafts"), payload), 403);
        request(root, body(post("/articles/drafts"), Map.of("title", marker, "content", "draft")), 400);
        request(root, body(post("/articles/drafts"), Map.of("title", "x".repeat(256), "content", "draft", "categoryId", category.getId())), 400);
        var draft = article(0);
        request(root, body(put("/articles/edit"), Map.of("id", draft.getId(), "title", "x".repeat(256), "content", "draft", "categoryId", category.getId())), 400);
    }

    private Article ownedDraft(User owner) {
        var draft = article(0).setUserId(owner.getId()).setAuthor(owner.getUsername());
        articles.update(draft);
        return draft;
    }

    private Map<String, Object> editPayload(Article article, Category category) {
        return Map.of("id", article.getId(), "title", marker, "content", "edited draft",
                "categoryId", category.getId(), "tagIds", List.of());
    }

    @Test void anAuthorCanCreateAndEditADraftWithoutPublishingOrExposingIt() throws Exception {
        var writer = writer(); var category = category("draft"); var tag = tag("draft");
        request(writer, body(post("/articles/create"), Map.of("title", marker, "content", "initial draft",
                "categoryId", category.getId(), "tagIds", List.of(tag.getId()))), 200);
        var draft = articles.selectOneByQuery(QueryWrapper.create().where(ARTICLE.TITLE.eq(marker)));
        assertNotNull(draft); articleIds.add(draft.getId());
        var nextCategory = category("next"); var nextTag = tag("next");
        request(writer, body(put("/articles/edit"), Map.of("id", draft.getId(), "title", marker,
                "content", "revised private draft", "categoryId", nextCategory.getId(), "tagIds", List.of(nextTag.getId()))), 200);
        var stored = Db.selectOneBySql("SELECT status,user_id,content FROM article WHERE id=?", draft.getId());
        assertEquals(0, stored.getInt("status")); assertEquals(writer.getId(), stored.getLong("user_id"));
        assertEquals("revised private draft", stored.getString("content"));
        var detail = data(request(writer, get("/articles/" + draft.getId()), 200));
        assertEquals(nextCategory.getId(), detail.path("categoryId").asLong());
        assertEquals(nextTag.getId(), detail.path("tagIds").get(0).asLong());
        assertFalse(Boolean.TRUE.equals(cache.hasKey(ARTICLE_DETAIL + draft.getId())));
        var reader = writer("reader", "article:list");
        request(reader, get("/articles/" + draft.getId()), 404);
        assertEquals(0, data(request(reader, get("/articles").param("title", marker), 200)).path("totalRow").asLong());
        assertEquals(0, data(request(writer, get("/articles/published").param("title", marker), 200)).path("totalRow").asLong());
    }

    @Test void anAuthorCanDeleteAnUnpublishedDraftWithTheDeletePermission() throws Exception {
        var writer = writer(); var draft = ownedDraft(writer);
        assertTrue(data(request(writer, delete("/articles/" + draft.getId()), 200)).asBoolean());
        assertEquals(1, Db.selectOneBySql("SELECT deleted FROM article WHERE id=?", draft.getId()).getInt("deleted"));
        request(writer, get("/articles/" + draft.getId()), 404);
    }

    @ParameterizedTest @ValueSource(longs = {1L, 2L})
    void administratorsCanEditAndDeleteAnotherAuthorsDraft(long roleId) throws Exception {
        var writer = writer(); var manager = user("manager", roleId); var draft = ownedDraft(writer);
        var category = category("managed");
        request(manager, body(put("/articles/edit"), editPayload(draft, category)), 200);
        var stored = Db.selectOneBySql("SELECT status,user_id,author FROM article WHERE id=?", draft.getId());
        assertEquals(0, stored.getInt("status")); assertEquals(writer.getId(), stored.getLong("user_id"));
        assertEquals(writer.getUsername(), stored.getString("author"));
        request(manager, delete("/articles/" + draft.getId()), 200);
        assertEquals(1, Db.selectOneBySql("SELECT deleted FROM article WHERE id=?", draft.getId()).getInt("deleted"));
    }

    @Test void havingWritePermissionsDoesNotAllowManagingSomeoneElsesDraft() throws Exception {
        var writer = writer(); var other = writer("other", "article:list", "article:update", "article:delete");
        var draft = ownedDraft(writer); var category = category("protected");
        request(other, body(put("/articles/edit"), editPayload(draft, category)), 403);
        request(other, delete("/articles/" + draft.getId()), 403);
        var stored = Db.selectOneBySql("SELECT status,deleted,content FROM article WHERE id=?", draft.getId());
        assertEquals(0, stored.getInt("status")); assertEquals(0, stored.getInt("deleted"));
        assertEquals("isolated audit content", stored.getString("content"));
    }

    @Test void ownershipWithoutTheCorrespondingPermissionsDoesNotPermitDraftWrites() throws Exception {
        var owner = writer("read_only", "article:list"); var draft = ownedDraft(owner);
        var category = category("readonly");
        request(owner, body(put("/articles/edit"), editPayload(draft, category)), 403);
        request(owner, delete("/articles/" + draft.getId()), 403);
        assertEquals(0, Db.selectOneBySql("SELECT deleted FROM article WHERE id=?", draft.getId()).getInt("deleted"));
    }

    @Test void theServiceAlsoRejectsDraftWritesWithoutTheRequiredPermissions() {
        var owner = writer("service_reader", "article:list"); var draft = ownedDraft(owner);
        var category = category("service");
        var principal = new com.nebula.auth.security.AuthLoginUser(owner, List.of("USER"), List.of("article:list"));
        SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> articleService.delete(draft.getId()));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> articleService.updateArticle(
                com.nebula.article.dto.UpdateArticleDto.create().setId(draft.getId()).setTitle(marker)
                        .setContent("blocked").setCategoryId(category.getId())));
    }

    @Test void anonymousClientsCannotEditOrDeleteDrafts() throws Exception {
        var draft = article(0); var category = category("anonymous");
        request(null, body(put("/articles/edit"), editPayload(draft, category)), 401);
        request(null, delete("/articles/" + draft.getId()), 401);
    }

    @Test void invalidCatalogReferencesDoNotChangeDraftContentOrBindings() throws Exception {
        var writer = writer(); var draft = ownedDraft(writer); var category = category("original");
        articleCategories.insert(ArticleCategory.create().setArticleId(draft.getId()).setCategoryId(category.getId()));
        request(writer, body(put("/articles/edit"), Map.of("id", draft.getId(), "title", marker, "content", "bad",
                "categoryId", Long.MAX_VALUE)), 400);
        request(writer, body(put("/articles/edit"), Map.of("id", draft.getId(), "title", marker, "content", "bad",
                "categoryId", category.getId(), "tagIds", List.of(Long.MAX_VALUE))), 400);
        var detail = data(request(writer, get("/articles/" + draft.getId()), 200));
        assertEquals("isolated audit content", detail.path("content").asText());
        assertEquals(category.getId(), detail.path("categoryId").asLong());
        assertEquals(0, detail.path("status").asInt());
    }

    @Test void aDeletedDraftCannotBeEditedOrDeletedAgain() throws Exception {
        var writer = writer(); var draft = ownedDraft(writer); var category = category("deleted");
        request(writer, delete("/articles/" + draft.getId()), 200);
        request(writer, body(put("/articles/edit"), editPayload(draft, category)), 404);
        request(writer, delete("/articles/" + draft.getId()), 404);
    }

    @Test void extraStatusAndOwnerFieldsCannotPublishOrTransferADraft() throws Exception {
        var writer = writer(); var draft = ownedDraft(writer); var category = category("forged");
        var payload = new LinkedHashMap<>(editPayload(draft, category));
        payload.put("status", 1); payload.put("userId", root.getId()); payload.put("author", root.getUsername());
        request(writer, body(put("/articles/edit"), payload), 200);
        var stored = Db.selectOneBySql("SELECT status,user_id,author FROM article WHERE id=?", draft.getId());
        assertEquals(0, stored.getInt("status")); assertEquals(writer.getId(), stored.getLong("user_id"));
        assertEquals(writer.getUsername(), stored.getString("author"));
    }

    @Test void aWithdrawnArticleCanBeEditedAndDeletedWithoutRepublishing() throws Exception {
        var writer = writer(); var article = ownedDraft(writer); var category = category("withdrawn");
        request(root, body(put("/articles/status"), Map.of("id", article.getId(), "status", 1)), 200);
        request(root, body(put("/articles/status"), Map.of("id", article.getId(), "status", 0)), 200);
        request(writer, body(put("/articles/edit"), editPayload(article, category)), 200);
        assertEquals(0, Db.selectOneBySql("SELECT status FROM article WHERE id=?", article.getId()).getInt("status"));
        request(writer, delete("/articles/" + article.getId()), 200);
    }

    @Test void draftWritingPermissionDoesNotGrantPublishingPermission() throws Exception {
        var writer = writer(); var draft = ownedDraft(writer);
        request(writer, body(put("/articles/status"), Map.of("id", draft.getId(), "status", 1)), 403);
        assertEquals(0, Db.selectOneBySql("SELECT status FROM article WHERE id=?", draft.getId()).getInt("status"));
    }

    @Test void blankRequiredFieldsDoNotEraseAnExistingDraft() throws Exception {
        var writer = writer(); var draft = ownedDraft(writer); var category = category("blank");
        request(writer, body(put("/articles/edit"), Map.of("id", draft.getId(), "title", " ", "content", "bad",
                "categoryId", category.getId())), 400);
        request(writer, body(put("/articles/edit"), Map.of("id", draft.getId(), "title", marker, "content", " ",
                "categoryId", category.getId())), 400);
        assertEquals("isolated audit content", Db.selectOneBySql("SELECT content FROM article WHERE id=?", draft.getId()).getString("content"));
    }
}
