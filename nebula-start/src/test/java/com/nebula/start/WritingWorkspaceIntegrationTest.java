package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.row.Db;
import com.nebula.article.entity.Article;
import com.nebula.article.entity.ArticleCategory;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.article.mapper.ArticleCategoryMapper;
import com.nebula.auth.util.JwtUtil;
import com.nebula.category.entity.Category;
import com.nebula.category.mapper.CategoryMapper;
import com.nebula.user.entity.User;
import com.nebula.user.entity.UserRole;
import com.nebula.user.mapper.UserMapper;
import com.nebula.user.mapper.UserRoleMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipInputStream;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 仅使用事务内专用账号和文章，验证备份保密、并发保护及正文不受影响。 */
@SpringBootTest(properties = "nebula.publication.enabled=false")
@AutoConfigureMockMvc
@Transactional
class WritingWorkspaceIntegrationTest extends JwtIntegrationTestSupport {
    static final Path ROOT = Path.of("target/writing-tests-" + UUID.randomUUID()).toAbsolutePath().normalize();
    @DynamicPropertySource static void files(DynamicPropertyRegistry registry) { registry.add("file.upload-root", ROOT::toString); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserMapper users;
    @Autowired UserRoleMapper roles;
    @Autowired ArticleMapper articles;
    @Autowired ArticleCategoryMapper bindings;
    @Autowired CategoryMapper categories;
    @Autowired JwtUtil jwt;
    @Autowired com.nebula.article.service.ArticleService articleService;
    @Autowired com.nebula.media.service.ImageStorage storage;
    @Autowired com.nebula.article.service.ArticleWorkspaceService workspaces;
    @Autowired com.nebula.article.service.ArticleReadingService reading;
    @Autowired com.nebula.article.service.ArticleSeriesService series;
    User owner, other;
    Category category;
    final List<String> tokens = new ArrayList<>();

    @BeforeEach void setup() {
        owner = user(3L); other = user(3L);
        category = Category.create().setName("备份回归" + UUID.randomUUID()).setDeleted(0);
        categories.insert(category);
    }
    /** 创建本次事务内的专用测试账号，不使用真实账号。 */
    private User user(long role) {
        String marker = "ws" + UUID.randomUUID().toString().replace("-", "");
        User user = User.create().setNid(marker).setUsername(marker).setEmail(marker + "@example.invalid")
                .setPassword("unused").setStatus(0).setDeleted(0);
        users.insert(user); roles.insert(UserRole.create().setUserId(user.getId()).setRoleId(role));
        return user;
    }
    /** 创建具有有效目录关系的测试文章，便于验证完整的手动保存链路。 */
    private Article article(int status) {
        Article a = Article.create().setUserId(owner.getId()).setAuthor(owner.getUsername())
                .setTitle("原始标题").setContent("原始正文").setStatus(status).setDeleted(0).setEditVersion(0L).setLikeCount(0).setViewCount(0L);
        articles.insert(a); bindings.insert(ArticleCategory.create().setArticleId(a.getId()).setCategoryId(category.getId()));
        return a;
    }
    /** 每次创建测试令牌，测试结束后撤销。 */
    private String auth(User user) { String token = jwt.createToken(user); tokens.add(token); return "Bearer " + token; }
    /** 备份可以不选分类，也可以保存尚未完成的空正文。 */
    private String backup(long revision, String content) throws Exception {
        return json.writeValueAsString(Map.of("revision", revision, "draft", Map.of("title", "备份标题", "content", content,
                "tagIds", List.of(), "sourceVersion", 0)));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); tokens.forEach(jwt::revokeToken); }

    @Test void delayedRequestsCannotRecreatePrivateDataAfterAccountCancellation() {
        Article a = article(1); asOwner();
        Db.updateBySql("UPDATE user SET deleted=1 WHERE id=?", owner.getId());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> workspaces.save("new", new com.nebula.article.dto.WorkspaceDto(0L, null)));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> reading.flags(a.getId(), new com.nebula.article.dto.ReadingDto.Flags(true, false)));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> reading.progress(a.getId(), new com.nebula.article.dto.ReadingDto.Progress(5000, "")));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> series.save(null, new com.nebula.article.dto.ArticleSeriesDto("不能复活", "", false, List.of(a.getId()))));
        assertNull(Db.selectOneBySql("SELECT * FROM article_workspace WHERE user_id=?", owner.getId()));
        assertNull(Db.selectOneBySql("SELECT * FROM article_reading WHERE user_id=?", owner.getId()));
        assertNull(Db.selectOneBySql("SELECT * FROM article_series WHERE user_id=?", owner.getId()));
    }

    /** 测试上传仍经过完整图片校验，事务回滚后自动删除专用文件。 */
    private com.nebula.media.entity.MediaAsset upload(User user) throws Exception {
        var response = mvc.perform(multipart("/media/images").file(new org.springframework.mock.web.MockMultipartFile("file", "回归图片.png", "image/png", MediaIntegrationTest.png()))
                .header("Authorization", auth(user))).andExpect(status().isOk()).andReturn();
        return json.treeToValue(json.readTree(response.getResponse().getContentAsString()).get("data"), com.nebula.media.entity.MediaAsset.class);
    }
    /** 服务层批量回归只使用本人身份，避免用HTTP频率预算干扰版本保留测试。 */
    private void asOwner() {
        var principal = new com.nebula.auth.security.AuthLoginUser(owner, List.of("USER"), List.of("article:list", "article:create", "article:update"));
        SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()));
    }
    @AfterAll static void removeEmptyFixtureDirectories() throws Exception {
        assertTrue(ROOT.startsWith(Path.of("target").toAbsolutePath().normalize()));
        assertTrue(ROOT.getFileName().toString().startsWith("writing-tests-"));
        if (Files.exists(ROOT)) try (var paths = Files.walk(ROOT)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    @Test void backupsNeverModifyPublishedContentOrSchedules() throws Exception {
        for (int status : List.of(0, 1)) {
            Article a = article(status);
            Db.updateBySql("UPDATE article SET scheduled_publish_at='2099-01-01 12:00:00',scheduled_by=? WHERE id=?", owner.getId(), a.getId());
            String path = "/article-workspaces/article-" + a.getId();
            mvc.perform(put(path).header("Authorization", auth(owner)).contentType(MediaType.APPLICATION_JSON).content(backup(0, "新的私人正文")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.revision").value(1));
            Article unchanged = articles.selectOneById(a.getId());
            assertEquals("原始正文", unchanged.getContent()); assertEquals(status, unchanged.getStatus());
            assertNotNull(unchanged.getScheduledPublishAt()); assertEquals(0L, unchanged.getEditVersion());
        }
    }

    @Test void staleSavesAndClearsCannotOverwriteAnotherPage() throws Exception {
        String token = auth(owner), path = "/article-workspaces/new";
        mvc.perform(put(path).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(backup(0, "第一页"))).andExpect(status().isOk());
        mvc.perform(put(path).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(backup(0, "过期页"))).andExpect(status().isConflict());
        mvc.perform(put(path).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{\"revision\":1,\"draft\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.revision").value(2));
        mvc.perform(put(path).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(backup(1, "不能复活旧内容"))).andExpect(status().isConflict());
        mvc.perform(get(path).header("Authorization", token)).andExpect(jsonPath("$.data.draft").isEmpty()).andExpect(jsonPath("$.data.revision").value(2));
    }

    @Test void anotherAccountCannotReadOrWriteTheOwnersBackup() throws Exception {
        Article a = article(1); String path = "/article-workspaces/article-" + a.getId();
        mvc.perform(put(path).header("Authorization", auth(owner)).contentType(MediaType.APPLICATION_JSON).content(backup(0, "私密"))).andExpect(status().isOk());
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization", auth(other))).andExpect(status().isForbidden());
        mvc.perform(put(path).header("Authorization", auth(other)).contentType(MediaType.APPLICATION_JSON).content(backup(1, "越权"))).andExpect(status().isForbidden());
        User admin = user(1L);
        mvc.perform(get(path).header("Authorization", auth(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.data.draft").isEmpty());
    }

    @Test void incompleteContentIsAcceptedButOversizedAndMalformedBackupsAreRejected() throws Exception {
        String token = auth(owner);
        mvc.perform(put("/article-workspaces/new").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(backup(0, ""))).andExpect(status().isOk());
        mvc.perform(put("/article-workspaces/new").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(backup(1, "字".repeat(200001)))).andExpect(status().isBadRequest());
        mvc.perform(get("/article-workspaces/article-0").header("Authorization", token)).andExpect(status().isBadRequest());
    }

    @Test void manualSaveRejectsAnOldEditorVersion() throws Exception {
        Article a = article(0); String token = auth(owner);
        String data = json.writeValueAsString(Map.of("id", a.getId(), "title", "修改后", "content", "新的正文", "categoryId", category.getId(), "expectedEditVersion", 0));
        mvc.perform(put("/articles/edit").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(data)).andExpect(status().isOk());
        mvc.perform(put("/articles/edit").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(data)).andExpect(status().isConflict());
        assertEquals(1L, articles.selectOneById(a.getId()).getEditVersion());
    }

    @Test void readingFlagsArePrivateAndIdempotent() throws Exception {
        Article a = article(1); String token = auth(owner), path = "/reading/" + a.getId();
        for (int i = 0; i < 2; i++) mvc.perform(put(path + "/flags").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"favorite\":true,\"later\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.favorite").value(true));
        assertEquals(1L, Db.selectOneBySql("SELECT COUNT(*) total FROM article_reading WHERE user_id=? AND article_id=?", owner.getId(), a.getId()).getLong("total"));
        mvc.perform(get(path).header("Authorization", auth(other))).andExpect(jsonPath("$.data.favorite").value(false));
        mvc.perform(get("/reading?kind=favorite").header("Authorization", auth(other))).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @Test void clearingHistoryKeepsFavoritesAndLaterButRemovesResumePositions() throws Exception {
        Article a = article(1); String token = auth(owner), path = "/reading/" + a.getId();
        mvc.perform(put(path + "/flags").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"favorite\":true,\"later\":true}")).andExpect(status().isOk());
        mvc.perform(put(path + "/progress").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"progress\":6543,\"anchor\":\"chapter-2\"}")).andExpect(status().isOk());
        mvc.perform(get("/reading?kind=history").header("Authorization", token)).andExpect(jsonPath("$.data.records[0].state.progress").value(6543));
        mvc.perform(delete("/reading/history").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get(path).header("Authorization", token)).andExpect(jsonPath("$.data.favorite").value(true))
                .andExpect(jsonPath("$.data.later").value(true)).andExpect(jsonPath("$.data.progress").value(0)).andExpect(jsonPath("$.data.readAt").isEmpty());
    }

    @Test void withdrawnAndDeletedArticlesDisappearFromLibraryAndCannotBeTracked() throws Exception {
        String token = auth(owner);
        for (int status : List.of(0, 1)) {
            Article a = article(status); String path = "/reading/" + a.getId();
            if (status == 0) {
                mvc.perform(get(path).header("Authorization", token)).andExpect(status().isNotFound());
                mvc.perform(put(path + "/progress").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progress\":100,\"anchor\":\"\"}")).andExpect(status().isNotFound());
            } else {
                mvc.perform(put(path + "/flags").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"favorite\":true,\"later\":false}")).andExpect(status().isOk());
                Db.updateBySql("UPDATE article SET status=0 WHERE id=?", a.getId());
                mvc.perform(get("/reading?kind=favorite").header("Authorization", token)).andExpect(jsonPath("$.data.total").value(0));
                Db.updateBySql("UPDATE article SET status=1,deleted=1 WHERE id=?", a.getId());
                mvc.perform(get("/reading?kind=favorite").header("Authorization", token)).andExpect(jsonPath("$.data.total").value(0));
            }
        }
    }

    @Test void readingParametersAreBounded() throws Exception {
        Article a = article(1); String token = auth(owner);
        mvc.perform(put("/reading/" + a.getId() + "/progress").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"progress\":10001,\"anchor\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/reading?kind=arbitrary").header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(get("/reading?size=100000").header("Authorization", token)).andExpect(status().isBadRequest());
    }

    @Test void revisionsStayPrivateAndReadingOneDoesNotRestoreOnlineContent() throws Exception {
        Article a = article(1); String token = auth(owner);
        String data = json.writeValueAsString(Map.of("id", a.getId(), "title", "新标题", "content", "已更新的正文", "categoryId", category.getId(), "expectedEditVersion", 0));
        mvc.perform(put("/articles/edit").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(data)).andExpect(status().isOk());
        var list = mvc.perform(get("/articles/" + a.getId() + "/revisions").header("Authorization", token)).andExpect(status().isOk()).andReturn();
        var versions = json.readTree(list.getResponse().getContentAsString()).get("data");
        assertEquals(2, versions.size()); long old = versions.get(1).get("id").asLong();
        String path = "/articles/" + a.getId() + "/revisions/" + old;
        mvc.perform(get(path).header("Authorization", auth(other))).andExpect(status().isForbidden());
        mvc.perform(get(path).header("Authorization", token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.draft.content").value("原始正文"));
        assertEquals("已更新的正文", articles.selectOneById(a.getId()).getContent());
        assertEquals(1, articles.selectOneById(a.getId()).getStatus());
    }

    @Test void manualRevisionHistoryKeepsOnlyLatestThirtyVersions() {
        Article a = article(0); asOwner();
        for (long version = 0; version < 34; version++) articleService.updateArticle(com.nebula.article.dto.UpdateArticleDto.create().setId(a.getId())
                .setTitle("第" + version + "次保存").setContent("版本正文" + version).setCategoryId(category.getId()).setTagIds(List.of()).setExpectedEditVersion(version));
        var rows = Db.selectListBySql("SELECT edit_version FROM article_revision WHERE article_id=? ORDER BY edit_version", a.getId());
        assertEquals(30, rows.size()); assertEquals(5L, rows.get(0).getLong("edit_version")); assertEquals(34L, rows.get(29).getLong("edit_version"));
    }

    @Test void workspaceAndRevisionReferencesProtectImagesWithoutMakingThemPublic() throws Exception {
        var asset = upload(owner); String token = auth(owner), content = "![私人图片](" + asset.getPath() + ")";
        mvc.perform(put("/article-workspaces/new").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(backup(0, content))).andExpect(status().isOk());
        mvc.perform(get("/" + asset.getPath())).andExpect(status().isNotFound());
        mvc.perform(delete("/media/" + asset.getId()).header("Authorization", token)).andExpect(status().isConflict());
        mvc.perform(put("/article-workspaces/new").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{\"revision\":1,\"draft\":null}")).andExpect(status().isOk());
        Article a = article(0);
        for (int version = 0; version < 2; version++) {
            String data = json.writeValueAsString(Map.of("id", a.getId(), "title", "历史配图", "content", version == 0 ? content : "已移除图片的当前正文", "categoryId", category.getId(), "expectedEditVersion", version));
            mvc.perform(put("/articles/edit").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(data)).andExpect(status().isOk());
        }
        mvc.perform(get("/" + asset.getPath())).andExpect(status().isNotFound());
        mvc.perform(get("/media/image").param("path", asset.getPath()).header("Authorization", auth(other))).andExpect(status().isNotFound());
        mvc.perform(delete("/media/" + asset.getId()).header("Authorization", token)).andExpect(status().isConflict());
    }

    @Test void seriesArePrivateByDefaultAndPublishOnlyOrderedPublishedArticles() throws Exception {
        Article first = article(1), draft = article(0), last = article(1); String token = auth(owner);
        String input = json.writeValueAsString(Map.of("title", "系列回归", "description", "按顺序阅读", "publicVisible", false, "articleIds", List.of(last.getId(), draft.getId(), first.getId())));
        var result = mvc.perform(post("/series").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isOk()).andReturn();
        long id = json.readTree(result.getResponse().getContentAsString()).get("data").asLong();
        mvc.perform(get("/series/" + id).header("Authorization", auth(other))).andExpect(status().isNotFound());
        mvc.perform(get("/series/" + id + "/edit").header("Authorization", auth(other))).andExpect(status().isForbidden());
        mvc.perform(put("/series/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(input.replace("false", "true"))).andExpect(status().isOk());
        mvc.perform(get("/series/" + id).header("Authorization", auth(other))).andExpect(jsonPath("$.data.articles.length()").value(2))
                .andExpect(jsonPath("$.data.articles[0].id").value(last.getId())).andExpect(jsonPath("$.data.articles[1].id").value(first.getId()));
        mvc.perform(get("/series/for-article/" + draft.getId()).header("Authorization", token)).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(delete("/series/" + id).header("Authorization", auth(other))).andExpect(status().isForbidden());
        mvc.perform(delete("/series/" + id).header("Authorization", token)).andExpect(status().isOk());
        assertNotNull(articles.selectOneById(first.getId()));
    }

    @Test void seriesRejectForeignArticlesAndDuplicateIds() throws Exception {
        Article a = article(1); String input = json.writeValueAsString(Map.of("title", "无权编排", "description", "", "publicVisible", true, "articleIds", List.of(a.getId())));
        mvc.perform(post("/series").header("Authorization", auth(other)).contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isBadRequest());
        input = json.writeValueAsString(Map.of("title", "重复编号", "description", "", "publicVisible", true, "articleIds", List.of(a.getId(), a.getId())));
        mvc.perform(post("/series").header("Authorization", auth(owner)).contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isBadRequest());
    }

    @Test void markdownAndImageArchiveExportEnforceArticleAndMediaVisibility() throws Exception {
        Article a = article(0); String token = auth(owner);
        mvc.perform(get("/articles/" + a.getId() + "/export").header("Authorization", auth(other))).andExpect(status().isNotFound());
        var markdown = mvc.perform(get("/articles/" + a.getId() + "/export").header("Authorization", token)).andExpect(status().isOk()).andReturn();
        assertEquals("# 原始标题\n\n原始正文", markdown.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertTrue(markdown.getResponse().getHeader("Content-Disposition").startsWith("attachment;"));
        var asset = upload(owner);
        var foreign = upload(other);
        String external = "https://example.invalid/" + asset.getPath();
        String content = "![本地](" + asset.getPath() + ")\n![外部](" + external + ")";
        var archive = mvc.perform(post("/article-export").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("title", "打包测试", "content", content)))).andExpect(status().isOk()).andReturn();
        Map<String,byte[]> entries = new HashMap<>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(archive.getResponse().getContentAsByteArray()), StandardCharsets.UTF_8)) {
            java.util.zip.ZipEntry entry; while ((entry = zip.getNextEntry()) != null) entries.put(entry.getName(), zip.readAllBytes());
        }
        assertEquals(2, entries.size()); String text = new String(entries.get("article.md"), StandardCharsets.UTF_8);
        assertTrue(text.contains("![本地](images/1-")); assertTrue(text.contains(external));
        mvc.perform(post("/article-export").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("title", "越权图片", "content", "![图](" + foreign.getPath() + ")")))).andExpect(status().isNotFound());
    }
}
