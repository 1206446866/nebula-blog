package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.row.Db;
import com.nebula.article.entity.*;
import com.nebula.article.mapper.*;
import com.nebula.article.service.ArticleTagService;
import com.nebula.article.vo.ArticleVO;
import com.nebula.auth.util.JwtUtil;
import com.nebula.common.constant.RoleEnum;
import com.nebula.common.mail.service.MailService;
import com.nebula.common.redis.RedisService;
import com.nebula.common.security.ResourceLimitProperties;
import com.nebula.tag.entity.Tag;
import com.nebula.tag.mapper.TagMapper;
import com.nebula.user.entity.*;
import com.nebula.user.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import static com.nebula.common.constant.redisKey.ArticleRedisKey.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 仅创建并清理本轮专用账号、标签和文章；真实HTTP覆盖编码路径与并发计数，不发邮件。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.address=127.0.0.1", "nebula.publication.enabled=false",
        "spring.mail.username=review@example.invalid", "spring.mail.password=unused-fixture"})
@AutoConfigureMockMvc
class ReviewFixesIntegrationTest extends JwtIntegrationTestSupport {
    private static final String PREFIX = "test:review:" + UUID.randomUUID() + ":";
    @DynamicPropertySource static void settings(DynamicPropertyRegistry registry) {
        registry.add("nebula.resources.key-prefix", () -> PREFIX);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserMapper users;
    @Autowired UserRoleMapper roles;
    @Autowired ArticleMapper articles;
    @Autowired ArticleTagMapper articleTags;
    @Autowired TagMapper tags;
    @Autowired ArticleTagService tagCounts;
    @Autowired JwtUtil jwt;
    @Autowired RedisService cache;
    @Autowired StringRedisTemplate redis;
    @Autowired ResourceLimitProperties rates;
    @MockitoBean MailService mail;
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    final List<Long> userIds = new ArrayList<>(), articleIds = new ArrayList<>(), tagIds = new ArrayList<>();
    final List<String> tokens = new ArrayList<>();
    String marker;

    @BeforeEach void setup() {
        marker = "review_" + UUID.randomUUID().toString().replace("-", "");
        rates.getSearch().setAccount(30); rates.getSearch().setIp(10000);
        rates.getWrite().setAccount(60); rates.getWrite().setIp(10000);
        rates.getUpload().setAccount(12); rates.getUpload().setIp(10000);
    }
    private User user(boolean root) {
        User user = User.create().setNid(marker + userIds.size()).setUsername("审查回归" + userIds.size())
                .setPassword("unused-fixture").setStatus(0).setDeleted(0);
        users.insert(user); userIds.add(user.getId());
        if (root) roles.insert(UserRole.create().setUserId(user.getId()).setRoleId(RoleEnum.SUPER_ADMIN.getRoleId()));
        return user;
    }
    private String bearer(User user) { String value = jwt.createToken(user); tokens.add(value); return "Bearer " + value; }
    private Article article(User user, int status) {
        Article article = Article.create().setUserId(user.getId()).setAuthor(user.getUsername()).setTitle(marker)
                .setContent("当前数据库正文").setStatus(status).setDeleted(0).setViewCount(0L).setLikeCount(0);
        articles.insert(article); articleIds.add(article.getId()); return article;
    }
    private HttpResponse<String> httpGet(String path, String authorization) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path))
                .header("Authorization",authorization).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void encodedSearchConsumesTheSameBudgetOverRealHttp() throws Exception {
        rates.getSearch().setAccount(1);
        String authorization = bearer(user(true));
        assertEquals(200,httpGet("/search",authorization).statusCode());
        var blocked = httpGet("/se%61rch",authorization);
        assertEquals(429,blocked.statusCode()); assertTrue(blocked.headers().firstValue("Retry-After").isPresent());
    }
    @Test void encodedUploadCannotSkipTheBudgetBeforeMultipartParsing() throws Exception {
        rates.getUpload().setAccount(1);
        String authorization = bearer(user(true));
        // 第一条格式错误的请求也占用预算，第二条编码路径必须先被限流而非进入解析。
        mvc.perform(post("/media/images").header("Authorization",authorization)).andExpect(status().isBadRequest());
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/m%65dia/images"))
                .header("Authorization",authorization).POST(HttpRequest.BodyPublishers.noBody()).build();
        assertEquals(429,HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString()).statusCode());
    }
    @Test void differentWriteEntrypointsShareAccountBudget() throws Exception {
        rates.getWrite().setAccount(2);
        String authorization = bearer(user(true));
        mvc.perform(put("/comments/release").header("Authorization",authorization).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/articles/drafts").header("Authorization",authorization).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/articles/like").header("Authorization",authorization).contentType("application/json").content("{}"))
                .andExpect(status().isTooManyRequests());
    }
    @Test void tagAggregationRequiresTheSamePermissionAsTagList() throws Exception {
        String authorization = bearer(user(false));
        mvc.perform(get("/tags/list").header("Authorization",authorization)).andExpect(status().isForbidden());
        mvc.perform(get("/api/tag").header("Authorization",authorization)).andExpect(status().isForbidden());
    }
    @Test void tagCountsExcludeDraftsAndDeletedArticles() throws Exception {
        User owner = user(true);
        Tag tag = Tag.create().setName(marker).setDeleted(0); tags.insert(tag); tagIds.add(tag.getId());
        for (int status : new int[]{1,0,1}) {
            Article article = article(owner,status);
            articleTags.insert(ArticleTag.create().setArticleId(article.getId()).setTagId(tag.getId()));
        }
        articles.deleteById(articleIds.get(2));
        assertEquals(1L,tagCounts.getArticleCountByTag().get(tag.getId()));
        var response = mvc.perform(get("/api/tag").header("Authorization",bearer(owner))).andExpect(status().isOk()).andReturn();
        var entries = json.readTree(response.getResponse().getContentAsString()).path("data");
        boolean found = false;
        for (var entry : entries) if (entry.path("id").asLong()==tag.getId()) { found=true; assertEquals(1,entry.path("articleCount").asInt()); }
        assertTrue(found);
    }
    @Test void staleCacheCannotOverrideCurrentDatabaseBody() throws Exception {
        User owner = user(true); Article article = article(owner,1); String authorization = bearer(owner);
        cache.set(ARTICLE_DETAIL+article.getId(),ArticleVO.create().setContent("过时正文"),30,TimeUnit.MINUTES);
        mvc.perform(get("/articles/"+article.getId()).header("Authorization",authorization))
                .andExpect(jsonPath("$.data.content").value("当前数据库正文"));
        articles.update(Article.create().setId(article.getId()).setContent("再次修改后的正文"));
        cache.set(ARTICLE_DETAIL+article.getId(),ArticleVO.create().setContent("迟到的旧正文"),30,TimeUnit.MINUTES);
        mvc.perform(get("/articles/"+article.getId()).header("Authorization",authorization))
                .andExpect(jsonPath("$.data.content").value("再次修改后的正文"));
    }
    @Test void concurrentViewsOfOneUserAreCountedOnlyOnce() throws Exception {
        User owner = user(true); Article article = article(owner,1); String authorization = bearer(owner);
        var executor = Executors.newFixedThreadPool(8); var start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        try {
            for (int i=0;i<8;i++) futures.add(executor.submit(()->{ start.await(); return httpGet("/articles/"+article.getId(),authorization).statusCode(); }));
            start.countDown();
            for (var future : futures) assertEquals(200,future.get(20,TimeUnit.SECONDS));
            assertEquals(1L,articles.selectOneById(article.getId()).getViewCount());
        } finally { start.countDown(); executor.shutdownNow(); assertTrue(executor.awaitTermination(10,TimeUnit.SECONDS)); }
    }
    @Test void compensationOnlyReleasesItsOwnRedisReservation() {
        String key = PREFIX+"reservation";
        assertTrue(cache.setIfAbsent(key,"first",60,TimeUnit.SECONDS));
        assertFalse(cache.setIfAbsent(key,"second",60,TimeUnit.SECONDS));
        assertFalse(cache.deleteIfValue(key,"second"));
        assertEquals("first",cache.get(key));
        assertTrue(cache.deleteIfValue(key,"first"));
        assertTrue(cache.setIfAbsent(key,"second",60,TimeUnit.SECONDS));
        assertFalse(cache.deleteIfValue(key,"first"));
        assertEquals("second",cache.get(key));
    }
    @Test void interfaceDocumentationIsNotAnonymousAndDisabledByDefault() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs").header("Authorization",bearer(user(true)))).andExpect(status().isNotFound());
    }

    @AfterEach void cleanup() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        tokens.forEach(jwt::revokeToken);
        for (Long id : articleIds) {
            var record = Db.selectOneBySql("SELECT user_id,title FROM article WHERE id=?",id);
            if (record == null) continue;
            assertTrue(userIds.contains(record.getLong("user_id"))); assertEquals(marker,record.getString("title"));
            Db.deleteBySql("DELETE FROM article_tag WHERE article_id=?",id);
            Db.deleteBySql("DELETE FROM article WHERE id=?",id);
            cache.delete(ARTICLE_DETAIL+id);
            for (Long user : userIds) cache.delete(ARTICLE_VIEW+id+":"+user);
        }
        for (Long id : tagIds) Db.deleteBySql("DELETE FROM tag WHERE id=? AND name=?",id,marker);
        for (Long id : userIds) {
            Db.deleteBySql("DELETE FROM user_role WHERE user_id=?",id);
            Db.deleteBySql("DELETE FROM user WHERE id=? AND nid LIKE ?",id,marker+"%");
        }
        var keys = redis.keys(PREFIX+"*"); if (keys!=null&&!keys.isEmpty()) redis.delete(keys);
    }
}
