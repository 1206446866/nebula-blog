package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.row.Db;
import com.nebula.article.entity.*;
import com.nebula.article.mapper.*;
import com.nebula.article.service.ArticlePublicationService;
import com.nebula.auth.util.JwtUtil;
import com.nebula.category.entity.Category;
import com.nebula.category.mapper.CategoryMapper;
import com.nebula.comment.entity.Comment;
import com.nebula.comment.mapper.CommentMapper;
import com.nebula.tag.entity.Tag;
import com.nebula.tag.mapper.TagMapper;
import com.nebula.user.entity.*;
import com.nebula.user.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 专用已提交账号用于跨事务轮询测试；仅清理本测试创建且校验过归属的ID。 */
@SpringBootTest(properties = "nebula.publication.enabled=false")
@AutoConfigureMockMvc
class PublicationAndSearchIntegrationTest extends JwtIntegrationTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserMapper users;
    @Autowired UserRoleMapper roles;
    @Autowired ArticleMapper articles;
    @Autowired ArticleCategoryMapper bindings;
    @Autowired CategoryMapper categories;
    @Autowired TagMapper tags;
    @Autowired CommentMapper comments;
    @Autowired JwtUtil jwt;
    @Autowired ArticlePublicationService publications;
    final List<Long> userIds = new ArrayList<>(), articleIds = new ArrayList<>(), commentIds = new ArrayList<>();
    final List<String> tokens = new ArrayList<>();
    String marker;
    User manager, owner;
    Category category;
    Tag tag;

    @BeforeEach void setup() {
        marker = "ps" + UUID.randomUUID().toString().replace("-", "");
        manager = user(1L); owner = user(3L);
        category = Category.create().setName(marker + "分类").setDescription("分类简介").setDeleted(0); categories.insert(category);
        tag = Tag.create().setName(marker + "标签").setDescription("标签简介").setDeleted(0); tags.insert(tag);
    }
    private User user(Long role) {
        var user = User.create().setNid(marker + "_" + userIds.size()).setUsername(marker + "用户" + userIds.size())
                .setPassword("unused").setEmail(UUID.randomUUID() + "@example.invalid").setStatus(0).setDeleted(0);
        users.insert(user); userIds.add(user.getId());
        roles.insert(UserRole.create().setUserId(user.getId()).setRoleId(role)); return user;
    }
    private Article article(int status) {
        var article = Article.create().setTitle(marker + "文章").setContent(marker + "正文").setUserId(owner.getId())
                .setAuthor(owner.getUsername()).setStatus(status).setDeleted(0).setViewCount(0L).setLikeCount(0);
        articles.insert(article); articleIds.add(article.getId());
        bindings.insert(ArticleCategory.create().setArticleId(article.getId()).setCategoryId(category.getId())); return article;
    }
    private void comment(Article article, int status) {
        var c = Comment.create().setArticleId(article.getId()).setUserId(owner.getId()).setContent(marker + "评论")
                .setParentId(0L).setStatus(status).setLikeCount(0L);
        comments.insert(c); commentIds.add(c.getId());
    }
    private String auth(User user) { String token = jwt.createToken(user); tokens.add(token); return "Bearer " + token; }
    private String path(Article a) { return "/articles/" + a.getId() + "/publication"; }
    private String future() { return OffsetDateTime.now(ZoneOffset.ofHours(8)).plusHours(1).withNano(0).toString(); }
    private void due(Article a) {
        Db.updateBySql("UPDATE article SET scheduled_publish_at=?,scheduled_by=?,schedule_error=NULL WHERE id=?", LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusMinutes(2), manager.getId(), a.getId());
    }
    /** 真实库中若另有到期计划，跳过轮询测试，避免测试触发他人的文章发布。 */
    private int publishFixturePlans() {
        String placeholders = String.join(",", Collections.nCopies(articleIds.size(), "?"));
        var otherPlans = Db.selectListBySql("SELECT id FROM article WHERE deleted=0 AND status=0 AND schedule_error IS NULL AND scheduled_publish_at<=? AND id NOT IN (" + placeholders + ")",
                java.util.stream.Stream.concat(java.util.stream.Stream.of(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))), articleIds.stream()).toArray());
        Assumptions.assumeTrue(otherPlans.isEmpty(), "存在真实到期计划，跳过全局轮询验收");
        return publications.publishDue();
    }
    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext(); tokens.forEach(jwt::revokeToken);
        for (Long id : commentIds) Db.deleteBySql("DELETE FROM comment WHERE id=? AND user_id=?", id, owner.getId());
        for (Long id : articleIds) {
            var row = Db.selectOneBySql("SELECT user_id FROM article WHERE id=?", id);
            assertEquals(owner.getId(), row.getLong("user_id"));
            Db.deleteBySql("DELETE FROM article_category WHERE article_id=?", id);
            Db.deleteBySql("DELETE FROM article_tag WHERE article_id=?", id);
            Db.deleteBySql("DELETE FROM article WHERE id=? AND user_id=?", id, owner.getId());
        }
        if (category != null) Db.deleteBySql("DELETE FROM category WHERE id=? AND name=?", category.getId(), category.getName());
        if (tag != null) Db.deleteBySql("DELETE FROM tag WHERE id=? AND name=?", tag.getId(), tag.getName());
        for (Long id : userIds) {
            Db.deleteBySql("DELETE FROM user_role WHERE user_id=?", id);
            Db.deleteBySql("DELETE FROM user WHERE id=? AND nid LIKE ?", id, marker + "%");
        }
    }

    @Test void schedulingNeedsAdminPermissionAndFutureOffsetTime() throws Exception {
        var a = article(0);
        mvc.perform(put(path(a)).contentType(MediaType.APPLICATION_JSON).content("{\"publishAt\":\"" + future() + "\"}")).andExpect(status().isUnauthorized());
        mvc.perform(put(path(a)).header("Authorization", auth(owner)).contentType(MediaType.APPLICATION_JSON).content("{\"publishAt\":\"" + future() + "\"}")).andExpect(status().isForbidden());
        for (String invalid : List.of("{}", "{\"publishAt\":\"2020-01-01T00:00:00+08:00\"}", "{\"publishAt\":\"2030-01-01T00:00:00\"}"))
            mvc.perform(put(path(a)).header("Authorization", auth(manager)).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
    }
    @Test void scheduleRescheduleAndCancelPersistWithoutPublishingTheDraft() throws Exception {
        var a = article(0); String token = auth(manager);
        String at = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2).withNano(0).toString();
        mvc.perform(put(path(a)).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{\"publishAt\":\"" + at + "\"}")).andExpect(status().isOk());
        assertEquals(0, articles.selectOneById(a.getId()).getStatus());
        assertEquals(OffsetDateTime.parse(at).atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime(), articles.selectOneById(a.getId()).getScheduledPublishAt());
        mvc.perform(put(path(a)).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{\"publishAt\":\"" + future() + "\"}")).andExpect(status().isOk());
        for (int i = 0; i < 2; i++) mvc.perform(delete(path(a)).header("Authorization", token)).andExpect(status().isOk());
        assertNull(articles.selectOneById(a.getId()).getScheduledPublishAt());
    }
    @Test void publishedOrIncompleteArticlesCannotBeScheduled() throws Exception {
        var a = article(1); String token = auth(manager);
        mvc.perform(put(path(a)).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{\"publishAt\":\"" + future() + "\"}")).andExpect(status().isBadRequest());
        a = article(0); Db.updateBySql("UPDATE article SET content='' WHERE id=?", a.getId());
        mvc.perform(put(path(a)).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{\"publishAt\":\"" + future() + "\"}")).andExpect(status().isBadRequest());
    }
    @Test void savingContentCancelsTheOldScheduleAndManualPublicationClearsIt() throws Exception {
        var a = article(0); due(a); String token = auth(manager);
        mvc.perform(put("/articles/edit").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("id", a.getId(), "title", a.getTitle(), "content", "新内容", "categoryId", category.getId(), "tagIds", List.of())))).andExpect(status().isOk());
        assertNull(articles.selectOneById(a.getId()).getScheduledPublishAt());
        due(a);
        mvc.perform(put("/articles/status").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{\"id\":" + a.getId() + ",\"status\":1}")).andExpect(status().isOk());
        assertNull(articles.selectOneById(a.getId()).getScheduledPublishAt());
        assertEquals(0, publishFixturePlans());
    }
    @Test void workerPublishesOverduePlansOnceAndLeavesFutureCancelledDeletedPlansPrivate() throws Exception {
        var ready = article(0); due(ready);
        var future = article(0); Db.updateBySql("UPDATE article SET scheduled_publish_at=?,scheduled_by=? WHERE id=?", LocalDateTime.now().plusDays(1), manager.getId(), future.getId());
        var deleted = article(0); due(deleted); articles.deleteById(deleted.getId());
        var cancelled = article(0); due(cancelled); mvc.perform(delete(path(cancelled)).header("Authorization", auth(manager))).andExpect(status().isOk());
        assertEquals(1, publishFixturePlans()); assertEquals(0, publishFixturePlans());
        assertEquals(1, articles.selectOneById(ready.getId()).getStatus());
        assertNull(articles.selectOneById(ready.getId()).getScheduledPublishAt());
        assertEquals(0, articles.selectOneById(future.getId()).getStatus()); assertEquals(0, articles.selectOneById(cancelled.getId()).getStatus());
    }
    @Test void revokedPublisherOrDisabledAuthorPausesInsteadOfPublishing() {
        var a = article(0); due(a);
        Db.deleteBySql("DELETE FROM user_role WHERE user_id=?", manager.getId());
        assertEquals(0, publishFixturePlans());
        assertNotNull(articles.selectOneById(a.getId()).getScheduleError());
        roles.insert(UserRole.create().setUserId(manager.getId()).setRoleId(1L));
        due(a); users.update(User.create().setId(owner.getId()).setStatus(1));
        assertEquals(0, publishFixturePlans()); assertEquals(0, articles.selectOneById(a.getId()).getStatus());
        assertNotNull(articles.selectOneById(a.getId()).getScheduleError());
    }
    @Test void twoWorkersDoNotPublishTheSameArticleTwice() throws Exception {
        var a = article(0); due(a); var pool = Executors.newFixedThreadPool(2);
        try {
            var release = new CountDownLatch(1); List<Future<Integer>> jobs = new ArrayList<>();
            for (int i = 0; i < 2; i++) jobs.add(pool.submit(() -> { release.await(); return publishFixturePlans(); }));
            release.countDown();
            assertEquals(1, jobs.get(0).get(15, TimeUnit.SECONDS) + jobs.get(1).get(15, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS)); }
    }
    @Test void ordinaryAdministratorWithUpdatePermissionCanExecuteAPlan() throws Exception {
        var a = article(0);
        Db.deleteBySql("DELETE FROM user_role WHERE user_id=?", manager.getId());
        roles.insert(UserRole.create().setUserId(manager.getId()).setRoleId(2L));
        mvc.perform(put(path(a)).header("Authorization", auth(manager)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"publishAt\":\"" + future() + "\"}")).andExpect(status().isOk());
        due(a);
        assertEquals(1, publishFixturePlans());
        assertEquals(1, articles.selectOneById(a.getId()).getStatus());
    }
    @Test void searchAggregatesAllFiveTypesButNeverReturnsPrivateOrDeletedContent() throws Exception {
        var pub = article(1); var draft = article(0); due(draft); var deleted = article(1); articles.deleteById(deleted.getId());
        comment(pub, 0); comment(pub, 1); comment(draft, 0);
        for (User viewer : List.of(owner, manager))
            mvc.perform(get("/search").header("Authorization", auth(viewer)).param("q", marker))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.counts.article").value(1))
                    .andExpect(jsonPath("$.data.counts.comment").value(1)).andExpect(jsonPath("$.data.counts.user").value(2))
                    .andExpect(jsonPath("$.data.counts.category").value(1)).andExpect(jsonPath("$.data.counts.tag").value(1))
                    .andExpect(jsonPath("$.data.page.totalRow").value(6))
                    .andExpect(jsonPath("$.data.page.records[*].password").isEmpty())
                    .andExpect(jsonPath("$.data.page.records[*].email").isEmpty());
    }
    @Test void searchUsesLiteralKeywordsAndSupportsContentMatchesAndPaging() throws Exception {
        var a = article(1); articles.update(Article.create().setId(a.getId()).setContent(marker + "%_正文"));
        String token = auth(owner);
        mvc.perform(get("/search").header("Authorization", token).param("q", marker + "%_").param("type", "article").param("current", "99").param("size", "1"))
                .andExpect(jsonPath("$.data.page.totalRow").value(1)).andExpect(jsonPath("$.data.page.pageNumber").value(1)).andExpect(jsonPath("$.data.page.records[0].id").value(a.getId()));
        mvc.perform(get("/search").header("Authorization", token).param("q", marker + "' OR 1=1 --")).andExpect(jsonPath("$.data.page.totalRow").value(0));
    }
    @Test void searchExcludesDeletedCatalogsAndUnavailableUsersAndHonorsReadPermissions() throws Exception {
        article(1); categories.deleteById(category.getId()); tags.deleteById(tag.getId()); users.update(User.create().setId(owner.getId()).setStatus(1));
        User reader = user(3L); Db.deleteBySql("DELETE FROM user_role WHERE user_id=?", reader.getId());
        mvc.perform(get("/search").header("Authorization", auth(reader)).param("q", marker))
                .andExpect(jsonPath("$.data.counts.article").value(0)).andExpect(jsonPath("$.data.counts.comment").value(0))
                .andExpect(jsonPath("$.data.counts.category").value(0)).andExpect(jsonPath("$.data.counts.tag").value(0))
                .andExpect(jsonPath("$.data.counts.user").value(2));
    }
    @Test void emptyAndInvalidSearchInputsAreHandledWithoutLeakingAllContent() throws Exception {
        String token = auth(owner);
        mvc.perform(get("/search")).andExpect(status().isUnauthorized());
        mvc.perform(get("/search").header("Authorization", token)).andExpect(jsonPath("$.data.page.totalRow").value(0));
        mvc.perform(get("/search").header("Authorization", token).param("q", "a".repeat(51))).andExpect(status().isBadRequest());
        mvc.perform(get("/search").header("Authorization", token).param("q", marker).param("type", "media")).andExpect(status().isBadRequest());
        mvc.perform(get("/search").header("Authorization", token).param("current", "0")).andExpect(status().isBadRequest());
    }
}
