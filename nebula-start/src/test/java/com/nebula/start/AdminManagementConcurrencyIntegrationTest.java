package com.nebula.start;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.nebula.article.dto.CreateArticleDto;
import com.nebula.article.dto.UpdateArticleDto;
import com.nebula.article.entity.Article;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.article.service.ArticleCatalogReferences;
import com.nebula.article.service.ArticleService;
import com.nebula.auth.AuthUserBuilder;
import com.nebula.category.entity.Category;
import com.nebula.category.mapper.CategoryMapper;
import com.nebula.category.service.CategoryService;
import com.nebula.comment.dto.CommentLikeDto;
import com.nebula.comment.entity.Comment;
import com.nebula.comment.mapper.CommentMapper;
import com.nebula.comment.service.CommentService;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.mail.service.MailService;
import com.nebula.common.redis.RedisService;
import com.nebula.role.dto.SaveRolePermissionsDto;
import com.nebula.role.entity.Role;
import com.nebula.role.entity.RolePermission;
import com.nebula.role.mapper.PermissionMapper;
import com.nebula.role.mapper.RoleMapper;
import com.nebula.role.mapper.RolePermissionMapper;
import com.nebula.role.service.PermissionService;
import com.nebula.role.service.RoleService;
import com.nebula.tag.entity.Tag;
import com.nebula.tag.mapper.TagMapper;
import com.nebula.tag.service.TagService;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.nebula.article.entity.table.ArticleTableDef.ARTICLE;
import static com.nebula.common.constant.redisKey.ArticleRedisKey.*;
import static com.nebula.role.entity.table.PermissionTableDef.PERMISSION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Committed, isolated fixtures exercise real InnoDB locking and after-commit callbacks. */
@SpringBootTest
class AdminManagementConcurrencyIntegrationTest extends JwtIntegrationTestSupport {
    @Autowired UserMapper users;
    @Autowired UserRoleMapper userRoles;
    @MockitoSpyBean ArticleMapper articles;
    @Autowired CategoryMapper categories;
    @Autowired TagMapper tags;
    @Autowired CommentMapper comments;
    @MockitoSpyBean RoleMapper roles;
    @Autowired PermissionMapper permissions;
    @Autowired RolePermissionMapper rolePermissions;
    @Autowired ArticleService articleService;
    @Autowired CommentService commentService;
    @Autowired CategoryService categoryService;
    @Autowired TagService tagService;
    @Autowired RoleService roleService;
    @Autowired PermissionService permissionService;
    @Autowired AuthUserBuilder principals;
    @MockitoSpyBean ArticleCatalogReferences references;
    @MockitoSpyBean RedisService cache;
    @Autowired StringRedisTemplate redis;
    @MockitoBean MailService mail;

    private final List<Long> categoryIds = new ArrayList<>();
    private final List<Long> tagIds = new ArrayList<>();
    private final List<Long> commentIds = new ArrayList<>();
    private final List<Long> roleIds = new ArrayList<>();
    private final List<CountDownLatch> releases = new ArrayList<>();
    private final ExecutorService workers = Executors.newFixedThreadPool(6);
    private String marker;
    private User root;

    @BeforeEach void setup() {
        marker = "mc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        root = User.create().setNid(marker).setUsername(marker).setPassword("isolated-committed-fixture").setStatus(0).setDeleted(0);
        users.insert(root); userRoles.insert(UserRole.create().setUserId(root.getId()).setRoleId(1L));
    }

    @AfterEach void cleanup() throws Exception {
        releases.forEach(CountDownLatch::countDown);
        workers.shutdownNow(); assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
        SecurityContextHolder.clearContext();
        for (var row : Db.selectListBySql("SELECT id FROM article WHERE user_id=?", root.getId())) {
            Long id = row.getLong("id");
            for (var comment : Db.selectListBySql("SELECT id FROM comment WHERE article_id=?", id)) {
                Db.deleteByMap("comment_like", Map.of("comment_id", comment.getLong("id")));
                Db.deleteById("comment", "id", comment.getLong("id"));
            }
            redis.delete(List.of(ARTICLE_DETAIL + id, ARTICLE_VIEW + id + ":" + root.getId()));
            Db.deleteByMap("article_category", Map.of("article_id", id));
            Db.deleteByMap("article_tag", Map.of("article_id", id));
            Db.deleteByMap("article_like", Map.of("article_id", id));
            Db.deleteById("article", "id", id);
        }
        for (Long id : commentIds) { Db.deleteByMap("comment_like", Map.of("comment_id", id)); Db.deleteById("comment", "id", id); }
        for (Long id : categoryIds) Db.deleteById("category", "id", id);
        for (Long id : tagIds) Db.deleteById("tag", "id", id);
        for (Long id : roleIds) {
            Db.deleteByMap("user_role", Map.of("role_id", id));
            Db.deleteByMap("role_permission", Map.of("role_id", id));
            Db.deleteById("role", "id", id);
        }
        Db.deleteByMap("user_role", Map.of("user_id", root.getId())); Db.deleteById("user", "id", root.getId());
        verifyNoInteractions(mail);
    }

    private <T> T asRoot(Callable<T> action) throws Exception {
        var principal = principals.createLoginUser(root);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        try { return action.call(); } finally { SecurityContextHolder.clearContext(); }
    }

    private <T> Future<T> start(String name, Callable<T> action) {
        return workers.submit(() -> { Thread.currentThread().setName(name + marker); return asRoot(action); });
    }

    private Category category() {
        var category = Category.create().setName(marker).setDescription("Committed fixture").setDeleted(0);
        categories.insert(category); categoryIds.add(category.getId()); return category;
    }

    private Tag tag() {
        var tag = Tag.create().setName(marker).setDescription("Committed fixture").setDeleted(0);
        tags.insert(tag); tagIds.add(tag.getId()); return tag;
    }

    private Article article() {
        var article = Article.create().setUserId(root.getId()).setAuthor(root.getUsername()).setTitle(marker)
                .setContent("original").setStatus(1).setDeleted(0).setLikeCount(0).setViewCount(0L);
        articles.insert(article); return article;
    }

    private void pause(CountDownLatch entered, CountDownLatch release) throws InterruptedException {
        entered.countDown(); assertTrue(release.await(8, TimeUnit.SECONDS));
    }

    @Test void concurrentCommentTogglesKeepRelationshipsAndCountsConsistent() throws Exception {
        var article = article();
        var comment = Comment.create().setArticleId(article.getId()).setUserId(root.getId()).setContent(marker).setStatus(0).setLikeCount(0L);
        comments.insert(comment); commentIds.add(comment.getId());
        for (int count : List.of(6, 5)) {
            var gate = new CountDownLatch(1); var futures = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < count; i++) futures.add(start("like", () -> {
                assertTrue(gate.await(5, TimeUnit.SECONDS));
                var dto = new CommentLikeDto(); dto.setCommentId(comment.getId()); return commentService.like(dto);
            }));
            gate.countDown(); for (var future : futures) future.get(10, TimeUnit.SECONDS);
            long expected = count == 6 ? 0 : 1;
            assertEquals(expected, Db.selectOneBySql("SELECT COUNT(*) AS total FROM comment_like WHERE comment_id=?", comment.getId()).getLong("total"));
            assertEquals(expected, Db.selectOneBySql("SELECT like_count FROM comment WHERE id=?", comment.getId()).getLong("like_count"));
        }
    }

    @ParameterizedTest @ValueSource(strings = {"category", "tag"})
    void aCommittedBindingPreventsAConcurrentCatalogDeletion(String kind) throws Exception {
        var category = category(); var tag = tag();
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); releases.add(release);
        var first = new AtomicBoolean(true);
        if (kind.equals("category")) {
            doAnswer(invocation -> { var result = invocation.callRealMethod();
                if (Thread.currentThread().getName().startsWith("bind") && first.getAndSet(false)) pause(entered, release);
                return result;
            }).when(references).lockCategory(category.getId());
        } else {
            doAnswer(invocation -> { var result = invocation.callRealMethod();
                if (Thread.currentThread().getName().startsWith("bind") && first.getAndSet(false)) pause(entered, release);
                return result;
            }).when(references).lockTags(anyList());
        }
        var binding = start("bind", () -> articleService.createArticle(new CreateArticleDto(marker, "new draft", category.getId(), List.of(tag.getId()))));
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        var deleting = start("catalog-delete", () -> kind.equals("category") ? categoryService.deleteCategory(category.getId()) : tagService.deleteTag(tag.getId()));
        assertThrows(TimeoutException.class, () -> deleting.get(200, TimeUnit.MILLISECONDS));
        release.countDown(); assertTrue(binding.get(10, TimeUnit.SECONDS));
        var failure = assertThrows(ExecutionException.class, () -> deleting.get(10, TimeUnit.SECONDS));
        assertInstanceOf(BusinessException.class, failure.getCause());
        assertEquals(409, ((BusinessException) failure.getCause()).getHttpStatus());
        assertNotNull(categories.selectOneById(category.getId())); assertNotNull(tags.selectOneById(tag.getId()));
    }

    @ParameterizedTest @ValueSource(ints = {0, 1})
    void articleDeletionSucceedsEvenWhenCacheEvictionFailsAndStaleCacheIsUnreadable(int status) throws Exception {
        var article = article(); String key = ARTICLE_DETAIL + article.getId();
        articles.update(Article.create().setId(article.getId()).setStatus(status));
        cache.set(key, com.nebula.article.vo.ArticleVO.create().setId(article.getId()).setContent("stale"));
        doReturn(false).when(cache).delete(key);
        assertTrue(asRoot(() -> articleService.delete(article.getId())));
        verify(cache).delete(key);
        assertEquals(1, Db.selectOneBySql("SELECT deleted FROM article WHERE id=?", article.getId()).getInt("deleted"));
        assertThrows(BusinessException.class, () -> asRoot(() -> articleService.getArticleById(article.getId())));
    }

    @ParameterizedTest @ValueSource(ints = {0, 1})
    void failedArticleDeletionDoesNotEvictTheCacheOrChangeTheRow(int status) throws Exception {
        var article = article(); String key = ARTICLE_DETAIL + article.getId();
        articles.update(Article.create().setId(article.getId()).setStatus(status));
        doReturn(0).when(articles).deleteById(article.getId());
        assertThrows(BusinessException.class, () -> asRoot(() -> articleService.delete(article.getId())));
        verify(cache, never()).delete(key);
        assertEquals(0, Db.selectOneBySql("SELECT deleted FROM article WHERE id=?", article.getId()).getInt("deleted"));
    }

    @ParameterizedTest @ValueSource(ints = {0, 1})
    void articleEditingEvictsDetailsOnlyAfterCommittedContentIsVisible(int status) throws Exception {
        var category = category(); var tag = tag(); var article = article();
        articles.update(Article.create().setId(article.getId()).setStatus(status));
        var observed = new AtomicReference<String>();
        doAnswer(invocation -> {
            observed.set(workers.submit(() -> Db.selectOneBySql("SELECT content FROM article WHERE id=?", article.getId())
                    .getString("content")).get(5, TimeUnit.SECONDS));
            return invocation.callRealMethod();
        }).when(cache).delete(ARTICLE_DETAIL + article.getId());
        var dto = UpdateArticleDto.create().setId(article.getId()).setTitle(marker).setContent("committed change")
                .setCategoryId(category.getId()).setTagIds(List.of(tag.getId()));
        assertTrue(asRoot(() -> articleService.updateArticle(dto)));
        assertEquals("committed change", observed.get());
        assertEquals(status, Db.selectOneBySql("SELECT status FROM article WHERE id=?", article.getId()).getInt("status"));
        assertEquals(1, Db.selectListBySql("SELECT category_id FROM article_category WHERE article_id=?", article.getId()).size());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void repliesAndRootDeletionSerializeWithoutLeavingOrphans(boolean replyFirst) throws Exception {
        var article = article();
        var parent = Comment.create().setArticleId(article.getId()).setUserId(root.getId()).setParentId(0L)
                .setContent(marker).setStatus(0).setLikeCount(0L);
        comments.insertSelective(parent); commentIds.add(parent.getId());
        var dto = new com.nebula.comment.dto.ReleaseCommentDto().setArticleId(article.getId()).setParentId(parent.getId()).setContent(marker + " reply");
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); releases.add(release);
        var first = new AtomicBoolean(true); String winner = replyFirst ? "reply" : "thread-delete";
        doAnswer(invocation -> {
            var row = invocation.callRealMethod();
            if (Thread.currentThread().getName().startsWith(winner) && first.getAndSet(false)) pause(entered, release);
            return row;
        }).when(articles).selectOneByQuery(any(QueryWrapper.class));
        var winning = start(winner, () -> replyFirst ? commentService.releaseComment(dto) : commentService.deleteCommentById(parent.getId()));
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        var losing = start(replyFirst ? "thread-delete" : "reply", () -> replyFirst ? commentService.deleteCommentById(parent.getId()) : commentService.releaseComment(dto));
        assertThrows(TimeoutException.class, () -> losing.get(200, TimeUnit.MILLISECONDS));
        release.countDown(); assertTrue(winning.get(10, TimeUnit.SECONDS));
        if (replyFirst) assertTrue(losing.get(10, TimeUnit.SECONDS));
        else assertInstanceOf(BusinessException.class, assertThrows(ExecutionException.class, () -> losing.get(10, TimeUnit.SECONDS)).getCause());
        assertEquals(0, Db.selectOneBySql("SELECT COUNT(*) AS total FROM comment WHERE article_id=? AND status=0", article.getId()).getInt("total"));
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void roleDeletionAndPermissionAssignmentSerializeWithoutLeavingBindings(boolean assignmentFirst) throws Exception {
        var role = Role.create().setName(marker).setDescription("Committed fixture").setDeleted(0);
        roles.insert(role); roleIds.add(role.getId());
        var permission = permissions.selectOneByCondition(PERMISSION.NAME.eq("tag:list")); assertNotNull(permission);
        rolePermissions.insert(RolePermission.create().setRoleId(role.getId()).setPermissionId(permission.getId()));
        var dto = new SaveRolePermissionsDto(); dto.setRoleId(role.getId()); dto.setPermissionIds(List.of(permission.getId()));
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); releases.add(release);
        var first = new AtomicBoolean(true); String winner = assignmentFirst ? "assign" : "role-delete";
        doAnswer(invocation -> {
            var row = invocation.callRealMethod();
            if (Thread.currentThread().getName().startsWith(winner) && first.getAndSet(false)) pause(entered, release);
            return row;
        }).when(roles).selectOneByQuery(any(QueryWrapper.class));
        Future<Boolean> winning = start(winner, () -> assignmentFirst ? permissionService.assignPermissionsToRole(dto) : roleService.removeRole(role.getId()));
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        Future<Boolean> losing = start(assignmentFirst ? "role-delete" : "assign", () -> assignmentFirst ? roleService.removeRole(role.getId()) : permissionService.assignPermissionsToRole(dto));
        assertThrows(TimeoutException.class, () -> losing.get(200, TimeUnit.MILLISECONDS));
        release.countDown(); assertTrue(winning.get(10, TimeUnit.SECONDS));
        if (assignmentFirst) assertTrue(losing.get(10, TimeUnit.SECONDS));
        else assertInstanceOf(BusinessException.class, assertThrows(ExecutionException.class, () -> losing.get(10, TimeUnit.SECONDS)).getCause());
        assertNull(roles.selectOneById(role.getId()));
        assertTrue(Db.selectListBySql("SELECT id FROM role_permission WHERE role_id=?", role.getId()).isEmpty());
    }
}
