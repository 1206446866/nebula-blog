package com.nebula.start;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.article.dto.ArticlePageDTO;
import com.nebula.article.entity.Article;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.article.service.ArticleService;
import com.nebula.article.vo.ArticleVO;
import com.nebula.auth.security.AuthLoginUser;
import com.nebula.auth.util.JwtUtil;
import com.nebula.comment.entity.Comment;
import com.nebula.comment.mapper.CommentLikeMapper;
import com.nebula.comment.mapper.CommentMapper;
import com.nebula.comment.service.CommentService;
import com.nebula.common.redis.RedisService;
import com.nebula.user.entity.User;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static com.nebula.comment.entity.table.CommentLikeTableDef.COMMENT_LIKE;
import static com.nebula.comment.entity.table.CommentTableDef.COMMENT;
import static com.nebula.common.constant.redisKey.ArticleRedisKey.ARTICLE_DETAIL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real MySQL and Security chain; every database fixture/write rolls back. Redis is isolated. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContentVisibilityIntegrationTest extends JwtIntegrationTestSupport {
    private final List<String> sessionTokens = new ArrayList<>();
    @Autowired UserMapper users;
    @Autowired UserRoleMapper userRoles;
    @Autowired ArticleMapper articles;
    @Autowired CommentMapper comments;
    @Autowired CommentLikeMapper commentLikes;
    @Autowired ArticleService articleService;
    @Autowired CommentService commentService;
    @Autowired JwtUtil jwt;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean RedisService redis;

    private String marker;
    private User author;
    private User reader;
    private Article published;
    private Article draft;
    private Article offline;
    private Article deleted;
    private Article ownDraft;
    private Comment publicComment;
    private Comment privateComment;
    private Comment offlineComment;
    private Comment deletedArticleComment;
    private Comment ownDraftComment;

    private User user(Long roleId) {
        User user = User.create().setNid("visibility_" + UUID.randomUUID())
                .setUsername("Visibility fixture").setPassword("unused-test-password-hash")
                .setStatus(0).setDeleted(0);
        users.insert(user);
        if (roleId != null) userRoles.insert(UserRole.create().setUserId(user.getId()).setRoleId(roleId));
        return user;
    }

    private Article article(User owner, String suffix, int status) {
        Article article = Article.create().setUserId(owner.getId()).setAuthor(owner.getUsername())
                .setTitle(marker + " " + suffix).setContent(marker + " body " + suffix)
                .setStatus(status).setDeleted(0).setViewCount(0L).setLikeCount(0);
        articles.insert(article);
        return article;
    }

    private Comment comment(Article article, String suffix, long likes) {
        // Being the comment author must not grant access to someone else's withdrawn article.
        Comment comment = Comment.create().setArticleId(article.getId()).setUserId(reader.getId())
                .setContent(marker + " comment " + suffix).setStatus(0).setLikeCount(likes);
        comments.insert(comment);
        return comment;
    }

    @BeforeEach
    void fixtures() {
        marker = "visibility_" + UUID.randomUUID().toString().replace("-", "");
        author = user(3L);
        reader = user(3L);
        published = article(author, "public", 1);
        draft = article(author, "private", 0);
        offline = article(author, "offline", 2);
        deleted = article(author, "deleted", 1);
        ownDraft = article(reader, "own", 0);
        publicComment = comment(published, "public", 4);
        privateComment = comment(draft, "private", 7);
        offlineComment = comment(offline, "offline", 11);
        deletedArticleComment = comment(deleted, "deleted", 13);
        ownDraftComment = comment(ownDraft, "own", 17);
        articles.deleteById(deleted.getId());
        when(redis.hasKey(anyString())).thenReturn(false);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
        sessionTokens.forEach(jwt::revokeToken);
    }

    private ResultActions as(User user, MockHttpServletRequestBuilder request) throws Exception {
        SecurityContextHolder.clearContext();
        String token = jwt.createToken(user);
        sessionTokens.add(token);
        return mvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString()).get("data");
    }

    private List<Long> ids(JsonNode page) {
        return StreamSupport.stream(page.get("records").spliterator(), false)
                .map(record -> record.get("id").asLong()).sorted().toList();
    }

    private String body(Object value) throws Exception { return json.writeValueAsString(value); }

    @Test
    void ordinaryArticleListsOnlyContainPublishedOrOwnUnpublishedArticles() throws Exception {
        var all = data(as(reader, get("/articles").param("title", marker)));
        assertEquals(List.of(published.getId(), ownDraft.getId()), ids(all));
        assertEquals(2L, all.get("totalRow").asLong());
        var spoofedOwner = data(as(reader, get("/articles").param("title", marker)
                .param("userId", author.getId().toString()).param("author", author.getUsername())));
        assertEquals(List.of(published.getId()), ids(spoofedOwner));
        assertEquals(List.of(ownDraft.getId()), ids(data(as(reader, get("/articles")
                .param("title", marker).param("userId", reader.getId().toString())))));
    }

    @Test
    void articleSearchAndPaginationDoNotRevealHiddenRowsOrCounts() throws Exception {
        var hidden = data(as(reader, get("/articles").param("title", draft.getTitle())));
        assertEquals(0L, hidden.get("totalRow").asLong());
        assertTrue(ids(hidden).isEmpty());
        var seen = new ArrayList<Long>();
        for (int page = 1; page <= 2; page++) {
            var result = data(as(reader, get("/articles").param("title", marker)
                    .param("current", Integer.toString(page)).param("size", "1")));
            assertEquals(2L, result.get("totalRow").asLong());
            seen.addAll(ids(result));
        }
        assertEquals(List.of(published.getId(), ownDraft.getId()), seen.stream().sorted().toList());
    }

    @ParameterizedTest
    @ValueSource(longs = {2L, 1L})
    void articleManagersCanReadUnpublishedContentButPublicListsStayPublic(long roleId) throws Exception {
        User manager = user(roleId);
        var result = data(as(manager, get("/articles").param("title", marker)
                .param("userId", author.getId().toString())));
        assertEquals(List.of(published.getId(), draft.getId(), offline.getId()), ids(result));
        for (Article privateArticle : List.of(draft, offline)) {
            as(manager, get("/articles/" + privateArticle.getId()))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.content").value(privateArticle.getContent()));
        }
        var publicOnly = data(as(manager, get("/articles/published").param("title", published.getTitle())));
        assertEquals(List.of(published.getId()), ids(publicOnly));
        assertTrue(ids(data(as(manager, get("/articles/published").param("title", draft.getTitle())))).isEmpty());
        var managerComments = data(as(manager, get("/comments").param("content", marker)));
        assertEquals(List.of(publicComment.getId(), privateComment.getId(), offlineComment.getId(), ownDraftComment.getId()), ids(managerComments));
        as(manager, get("/comments").param("articleId", deleted.getId().toString()))
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void privateMissingAndDeletedDetailsAreIndistinguishableEvenWithAWarmCache() throws Exception {
        when(redis.get(ARTICLE_DETAIL + draft.getId())).thenReturn(ArticleVO.create()
                .setId(draft.getId()).setContent("Previously cached secret").setStatus(1));
        for (long id : List.of(draft.getId(), offline.getId(), deleted.getId(), Long.MAX_VALUE)) {
            as(reader, get("/articles/" + id)).andExpect(jsonPath("$.code").value(40401))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
        verify(redis, never()).get(anyString());
        as(reader, get("/articles/" + published.getId())).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value(published.getContent()));
    }

    @Test
    void ownersCanPreviewDraftsWithoutSharedCachingOrPublicViewIncrements() throws Exception {
        for (Article article : List.of(draft, offline)) {
            as(author, get("/articles/" + article.getId())).andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.content").value(article.getContent()));
            assertEquals(0L, articles.selectOneById(article.getId()).getViewCount());
        }
        verifyNoInteractions(redis);
        as(author, get("/comments").param("articleId", draft.getId().toString()))
                .andExpect(jsonPath("$.data.records[0].content").value(privateComment.getContent()));
    }

    @Test
    void commentListsCannotBeBypassedByOmittingArticleIdOrByCommentOwnership() throws Exception {
        var all = data(as(reader, get("/comments").param("content", marker)));
        assertEquals(List.of(publicComment.getId(), ownDraftComment.getId()), ids(all));
        for (Article hidden : List.of(draft, offline, deleted)) {
            as(reader, get("/comments").param("articleId", hidden.getId().toString()))
                    .andExpect(jsonPath("$.code").value(40401));
        }
        var own = data(as(reader, get("/comments").param("articleId", ownDraft.getId().toString())));
        assertEquals(List.of(ownDraftComment.getId()), ids(own));
        User noBusinessPermissions = user(null);
        assertEquals(List.of(publicComment.getId()), ids(data(as(noBusinessPermissions,
                get("/comments").param("content", marker)))));
    }

    @Test
    void commentSearchAndPaginationCountsExcludePrivateComments() throws Exception {
        var hidden = data(as(reader, get("/comments").param("content", privateComment.getContent())));
        assertEquals(0L, hidden.get("totalRow").asLong());
        assertTrue(ids(hidden).isEmpty());
        var seen = new ArrayList<Long>();
        for (int page = 1; page <= 2; page++) {
            var result = data(as(reader, get("/comments").param("content", marker)
                    .param("currentPage", Integer.toString(page)).param("size", "1")));
            assertEquals(2L, result.get("totalRow").asLong());
            seen.addAll(ids(result));
        }
        assertEquals(List.of(publicComment.getId(), ownDraftComment.getId()), seen.stream().sorted().toList());
    }

    @Test
    void hiddenCommentsCannotBeCreatedLikedOrDeletedThroughGuessedIds() throws Exception {
        long previous = comments.selectCountByCondition(COMMENT.ARTICLE_ID.eq(draft.getId()));
        as(reader, put("/comments/release").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("articleId", draft.getId(), "content", "Must not be inserted"))))
                .andExpect(jsonPath("$.code").value(40401));
        as(reader, put("/comments/like").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("commentId", privateComment.getId(), "userId", reader.getId()))))
                .andExpect(jsonPath("$.code").value(40401));
        as(reader, delete("/comments/delete/" + privateComment.getId()))
                .andExpect(jsonPath("$.code").value(40401));
        assertEquals(previous, comments.selectCountByCondition(COMMENT.ARTICLE_ID.eq(draft.getId())));
        assertEquals(0L, commentLikes.selectCountByCondition(COMMENT_LIKE.COMMENT_ID.eq(privateComment.getId())));
        assertNotNull(comments.selectOneById(privateComment.getId()));
        as(reader, put("/comments/release").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("articleId", published.getId(), "content", marker + " new public comment"))))
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void profileQueriesTitlesAndAggregatesStayPublicWithoutARequestSecurityContext() {
        SecurityContextHolder.clearContext();
        var profileComments = commentService.getCommentPageByUserId(reader.getId(), 1, 100);
        assertEquals(List.of(publicComment.getId()), profileComments.getRecords().stream().map(Comment::getId).toList());
        assertEquals(1L, profileComments.getTotalRow());
        assertEquals(4L, commentService.getLikeAllCount(reader.getId()));
        assertEquals(Map.of(published.getId(), 1L), commentService.getCountByArticleIds(
                List.of(published.getId(), draft.getId(), offline.getId(), deleted.getId(), ownDraft.getId())));
        assertEquals(List.of(published.getId()), articleService.getArticleTitlesByCommentIds(
                List.of(published.getId(), draft.getId(), offline.getId(), deleted.getId(), ownDraft.getId()))
                .stream().map(Article::getId).toList());
    }

    @Test
    void withdrawalAndLogicalDeletionImmediatelyBlockPreviouslyCachedPublicContent() throws Exception {
        when(redis.get(ARTICLE_DETAIL + published.getId())).thenReturn(ArticleVO.create().setId(published.getId())
                .setUserId(author.getId()).setContent(published.getContent()).setStatus(1));
        as(reader, get("/articles/" + published.getId())).andExpect(jsonPath("$.code").value(200));
        clearInvocations(redis);
        articles.update(Article.create().setId(published.getId()).setStatus(0));
        as(reader, get("/articles/" + published.getId())).andExpect(jsonPath("$.code").value(40401));
        as(reader, get("/comments").param("articleId", published.getId().toString()))
                .andExpect(jsonPath("$.code").value(40401));
        verify(redis, never()).get(anyString());
        articles.update(Article.create().setId(published.getId()).setStatus(1));
        articles.deleteById(published.getId());
        as(author, get("/articles/" + published.getId())).andExpect(jsonPath("$.code").value(40401));
        assertTrue(ids(data(as(reader, get("/comments").param("content", publicComment.getContent())))).isEmpty());
    }

    @Test
    void directServiceReadsWithoutAuthenticationRemainPublicAndAnonymousHttpRemainsProtected() throws Exception {
        SecurityContextHolder.clearContext();
        assertEquals(List.of(published.getId()), articleService.pageArticles(ArticlePageDTO.create().setTitle(marker), null, true)
                .getRecords().stream().map(ArticleVO::getId).toList());
        assertEquals(List.of(publicComment.getId()), commentService.pageComments(null, marker, 1, 100)
                .getRecords().stream().map(comment -> comment.getId()).toList());
        mvc.perform(get("/articles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/comments")).andExpect(status().isUnauthorized());
    }

    @Test
    void paginationAndCommentTargetIdentifiersMustBeValid() throws Exception {
        for (String endpoint : List.of("/articles", "/articles/published", "/comments")) {
            as(reader, get(endpoint).param("size", "101")).andExpect(jsonPath("$.code").value(400));
        }
        as(reader, get("/articles").param("current", "0")).andExpect(jsonPath("$.code").value(400));
        as(reader, get("/comments").param("currentPage", "0")).andExpect(jsonPath("$.code").value(400));
        as(reader, get("/comments").param("articleId", "0")).andExpect(jsonPath("$.code").value(400));
        for (String invalid : List.of("{}", "{\"articleId\":0,\"content\":\"test\"}",
                "{\"articleId\":1,\"content\":\" \"}")) {
            as(reader, put("/comments/release").contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    @Test
    void anAdminRoleAloneWithoutArticleReadPermissionDoesNotExposeAllDrafts() {
        var principal = new AuthLoginUser(reader, List.of("ADMIN"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        assertEquals(List.of(published.getId(), ownDraft.getId()), articleService
                .pageArticles(ArticlePageDTO.create().setTitle(marker), null, true).getRecords()
                .stream().map(ArticleVO::getId).sorted().toList());
    }
}
