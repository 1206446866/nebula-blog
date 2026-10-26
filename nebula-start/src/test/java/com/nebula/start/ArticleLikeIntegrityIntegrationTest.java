package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.row.Db;
import com.nebula.article.dto.ArticleLikeDto;
import com.nebula.article.entity.Article;
import com.nebula.article.entity.ArticleLike;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.article.mapper.ArticleLikeMapper;
import com.nebula.article.service.ArticleService;
import com.nebula.article.vo.ArticleVO;
import com.nebula.auth.util.JwtUtil;
import com.nebula.common.redis.RedisService;
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
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.nebula.article.entity.table.ArticleLikeTableDef.ARTICLE_LIKE;
import static com.nebula.common.constant.redisKey.ArticleRedisKey.ARTICLE_DETAIL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 使用Flex和MySQL真实提交验证行锁，仅清理本测试创建的专用数据。 */
// 此处验证数据库的并发一致性；防刷额度由独立回归验证，避免高并发用例被正常限流挡住。
@SpringBootTest(properties = {"nebula.resources.write.concurrency=32", "nebula.resources.write.account-concurrency=32",
        "nebula.resources.write.ip-concurrency=32", "nebula.resources.max-concurrency=32"})
@AutoConfigureMockMvc
class ArticleLikeIntegrityIntegrationTest extends JwtIntegrationTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserMapper users;
    @Autowired UserRoleMapper userRoles;
    @Autowired ArticleLikeMapper likes;
    @MockitoSpyBean ArticleMapper articles;
    @Autowired ArticleService service;
    @Autowired JwtUtil jwt;
    @MockitoBean RedisService cache;
    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> articleIds = new ArrayList<>();
    private final List<String> sessionTokens = new ArrayList<>();

    private User user(long role) {
        var user = User.create().setNid("like_test_" + UUID.randomUUID()).setUsername("Article like fixture")
                .setPassword("unused-like-fixture-hash").setStatus(0).setDeleted(0);
        users.insert(user);
        userIds.add(user.getId());
        if (role != 0) userRoles.insert(UserRole.create().setUserId(user.getId()).setRoleId(role));
        return user;
    }

    private Article article(User author, int status) {
        var article = Article.create().setUserId(author.getId()).setAuthor(author.getUsername())
                .setTitle("Like fixture " + UUID.randomUUID()).setContent("Isolated article like test")
                .setStatus(status).setDeleted(0).setViewCount(0L).setLikeCount(0);
        articles.insert(article);
        articleIds.add(article.getId());
        return article;
    }

    private String token(User user) {
        String token = jwt.createToken(user);
        sessionTokens.add(token);
        return token;
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        sessionTokens.forEach(jwt::revokeToken);
        for (Long id : articleIds) {
            Db.deleteByMap("article_like", Map.of("article_id", id));
            Db.deleteById("article", "id", id);
        }
        for (Long id : userIds) {
            Db.deleteByMap("user_role", Map.of("user_id", id));
            Db.deleteById("user", "id", id);
        }
    }

    private boolean toggle(User actor, Article article, Map<String, Object> extra) throws Exception {
        SecurityContextHolder.clearContext();
        var body = new java.util.HashMap<String, Object>(extra);
        body.put("articleId", article.getId());
        var response = mvc.perform(put("/articles/like").header("Authorization", "Bearer " + token(actor))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).get("data").asBoolean();
    }

    private void consistent(Article article, int count) {
        assertEquals(count, articles.selectOneById(article.getId()).getLikeCount());
        assertEquals(count, likes.selectCountByCondition(ARTICLE_LIKE.ARTICLE_ID.eq(article.getId())));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 3, 2, 1})
    void noCallerIncludingAdministratorsCanLikeOnBehalfOfAnotherUser(long role) throws Exception {
        User actor = user(role);
        User other = user(3);
        Article article = article(other, 1);
        likes.insert(ArticleLike.create().setArticleId(article.getId()).setUserId(other.getId()));
        articles.update(Article.create().setId(article.getId()).setLikeCount(1));
        assertTrue(toggle(actor, article, Map.of("userId", other.getId())));
        consistent(article, 2);
        assertNotNull(likes.selectOneByCondition(ARTICLE_LIKE.ARTICLE_ID.eq(article.getId())
                .and(ARTICLE_LIKE.USER_ID.eq(actor.getId()))));
        assertFalse(toggle(actor, article, Map.of("userId", other.getId())));
        consistent(article, 1);
        assertNotNull(likes.selectOneByCondition(ARTICLE_LIKE.ARTICLE_ID.eq(article.getId())
                .and(ARTICLE_LIKE.USER_ID.eq(other.getId()))));
    }

    @Test
    void publishedArticleCanBeLikedByItsAuthorWithoutAUserIdInTheRequest() throws Exception {
        User actor = user(3);
        Article article = article(actor, 1);
        assertTrue(toggle(actor, article, Map.of()));
        consistent(article, 1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void draftAndWithdrawnArticlesCannotBeLikedEvenByTheirOwnerOrAnAdministrator(int status) throws Exception {
        User author = user(3);
        Article article = article(author, status);
        for (User actor : List.of(author, user(3), user(1))) {
            SecurityContextHolder.clearContext();
            mvc.perform(put("/articles/like").header("Authorization", "Bearer " + token(actor))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("articleId", article.getId()))))
                    .andExpect(jsonPath("$.code").value(40401));
        }
        consistent(article, 0);
    }

    @Test
    void deletedAndMissingArticlesShareTheSameFailureAndCannotAcquireLikes() throws Exception {
        User actor = user(1);
        Article article = article(actor, 1);
        articles.deleteById(article.getId());
        for (long id : List.of(article.getId(), Long.MAX_VALUE)) {
            SecurityContextHolder.clearContext();
            mvc.perform(put("/articles/like").header("Authorization", "Bearer " + token(actor))
                            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("articleId", id))))
                    .andExpect(jsonPath("$.code").value(40401));
            assertEquals(0, likes.selectCountByCondition(ARTICLE_LIKE.ARTICLE_ID.eq(id)));
        }
    }

    @Test
    void anonymousAndInvalidInputsCannotChangeAnyState() throws Exception {
        User actor = user(3);
        Article article = article(actor, 1);
        mvc.perform(put("/articles/like").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("articleId", article.getId()))))
                .andExpect(status().isUnauthorized());
        var dto = new ArticleLikeDto();
        dto.setArticleId(article.getId());
        assertThrows(AccessDeniedException.class, () -> service.like(dto));
        for (String body : List.of("{}", "{\"articleId\":0}", "{\"articleId\":-1}")) {
            mvc.perform(put("/articles/like").header("Authorization", "Bearer " + token(actor))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(jsonPath("$.code").value(400));
        }
        consistent(article, 0);
    }

    @Test
    void counterFailureRollsBackTheRelationshipAndDoesNotEvictBeforeCommit() throws Exception {
        User reader = user(3);
        Article article = article(user(3), 1);
        doReturn(0).when(articles).update(any(Article.class));
        mvc.perform(put("/articles/like").header("Authorization", "Bearer " + token(reader))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("articleId", article.getId()))))
                .andExpect(jsonPath("$.code").value(40901));
        consistent(article, 0);
        verify(cache, never()).delete(ARTICLE_DETAIL + article.getId());
        reset(articles);
        assertTrue(toggle(reader, article, Map.of()));
        consistent(article, 1);
        verify(cache).delete(ARTICLE_DETAIL + article.getId());
    }

    @Test
    void staleCachedCountersAndHistoricalOvercountsDoNotSurviveAnInteraction() throws Exception {
        User reader = user(3);
        Article article = article(user(3), 1);
        articles.update(Article.create().setId(article.getId()).setLikeCount(999));
        when(cache.get(ARTICLE_DETAIL + article.getId())).thenReturn(ArticleVO.create().setId(article.getId())
                .setUserId(article.getUserId()).setStatus(1).setContent("Cached fixture").setLikeCount(999).setLiked(false));
        assertTrue(toggle(reader, article, Map.of()));
        consistent(article, 1);
        mvc.perform(get("/articles/" + article.getId()).header("Authorization", "Bearer " + token(reader)))
                .andExpect(jsonPath("$.data.likeCount").value(1)).andExpect(jsonPath("$.data.liked").value(true));
        assertFalse(toggle(reader, article, Map.of()));
        consistent(article, 0);
        mvc.perform(get("/articles/" + article.getId()).header("Authorization", "Bearer " + token(reader)))
                .andExpect(jsonPath("$.data.likeCount").value(0)).andExpect(jsonPath("$.data.liked").value(false));
    }

    private List<Boolean> concurrent(Article article, List<User> actors) throws Exception {
        List<String> tokens = actors.stream().map(this::token).toList();
        var pool = Executors.newFixedThreadPool(actors.size());
        var start = new CountDownLatch(1);
        try {
            var tasks = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (String bearer : tokens) tasks.add(pool.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                var response = mvc.perform(put("/articles/like").header("Authorization", "Bearer " + bearer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(Map.of("articleId", article.getId()))))
                        .andExpect(jsonPath("$.code").value(200)).andReturn();
                return json.readTree(response.getResponse().getContentAsString()).get("data").asBoolean();
            }));
            start.countDown();
            var states = new ArrayList<Boolean>();
            for (var task : tasks) states.add(task.get(20, TimeUnit.SECONDS));
            return states;
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void concurrentReadersCanLikeAndCancelWithoutLosingCounts() throws Exception {
        Article article = article(user(3), 1);
        var readers = new ArrayList<User>();
        for (int i = 0; i < 8; i++) readers.add(user(3));
        assertTrue(concurrent(article, readers).stream().allMatch(Boolean::booleanValue));
        consistent(article, 8);
        assertTrue(concurrent(article, readers).stream().noneMatch(Boolean::booleanValue));
        consistent(article, 0);
    }

    @Test
    void concurrentTogglesFromTheSameReaderRemainConsistent() throws Exception {
        User reader = user(3);
        Article article = article(user(3), 1);
        assertEquals(4, concurrent(article, java.util.Collections.nCopies(8, reader)).stream().filter(Boolean::booleanValue).count());
        consistent(article, 0);
        assertEquals(5, concurrent(article, java.util.Collections.nCopies(9, reader)).stream().filter(Boolean::booleanValue).count());
        consistent(article, 1);
    }
}
