package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.article.entity.Article;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.auth.util.JwtUtil;
import com.nebula.category.entity.Category;
import com.nebula.category.mapper.CategoryMapper;
import com.nebula.media.entity.MediaAsset;
import com.nebula.media.mapper.MediaAssetMapper;
import com.nebula.media.service.ImageStorage;
import com.nebula.user.entity.*;
import com.nebula.user.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MediaIntegrationTest extends JwtIntegrationTestSupport {
    static final Path ROOT = Path.of("target/media-tests-" + UUID.randomUUID()).toAbsolutePath();
    @DynamicPropertySource static void mediaProperties(DynamicPropertyRegistry registry) { registry.add("file.upload-root", ROOT::toString); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserMapper users;
    @Autowired UserRoleMapper roles;
    @Autowired MediaAssetMapper assets;
    @Autowired ArticleMapper articles;
    @Autowired CategoryMapper categories;
    @Autowired JwtUtil jwt;
    @Autowired ImageStorage storage;
    @Autowired com.nebula.media.service.MediaService media;
    final List<String> tokens = new ArrayList<>();
    @AfterEach void revoke() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        tokens.forEach(jwt::revokeToken);
    }

    User user(Long role) {
        User user = User.create().setNid("media_test_" + UUID.randomUUID().toString().replace("-", ""))
                .setUsername("媒体测试").setPassword("unused-test-hash").setStatus(0).setDeleted(0);
        users.insertSelective(user);
        if (role != null) roles.insert(UserRole.create().setUserId(user.getId()).setRoleId(role));
        return user;
    }
    String bearer(User user) { String token = jwt.createToken(user); tokens.add(token); return "Bearer " + token; }
    static byte[] png() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(12, 8, BufferedImage.TYPE_INT_ARGB), "png", out);
        return out.toByteArray();
    }
    MediaAsset upload(User user) throws Exception {
        var result = mvc.perform(multipart("/media/images").file(new MockMultipartFile("file", "测试图.png", "image/png", png()))
                .header("Authorization", bearer(user))).andExpect(status().isOk()).andReturn();
        return json.treeToValue(json.readTree(result.getResponse().getContentAsString()).get("data"), MediaAsset.class);
    }
    Article reference(User owner, MediaAsset asset, int status) {
        Article article = Article.create().setTitle("媒体引用测试").setContent("![配图](/" + asset.getPath() + ")")
                .setAuthor(owner.getUsername()).setUserId(owner.getId()).setStatus(status).setDeleted(0);
        articles.insertSelective(article); return article;
    }

    @Test void uploadsNeedAuthenticationAndWritingPermission() throws Exception {
        mvc.perform(multipart("/media/images").file("file", png())).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/media/images").file(new MockMultipartFile("file", "a.png", "image/png", png()))
                .header("Authorization", bearer(user(null)))).andExpect(status().isForbidden());
    }
    @Test void pathUsesServerIdentityDateAndGeneratedName() throws Exception {
        User owner = user(3L); MediaAsset asset = upload(owner);
        assertTrue(asset.getPath().matches("upload/article/" + owner.getNid() + "/[0-9]{8}/[a-f0-9]{32}\\.png"));
        assertEquals(12, asset.getWidth()); assertEquals(8, asset.getHeight());
        assertTrue(Files.isRegularFile(storage.resolve(asset.getPath())));
        assertEquals(asset.getPath(), assets.selectOneById(asset.getId()).getPath());
    }
    @Test void spoofedSvgAndEmptyImagesAreRejected() throws Exception {
        String auth = bearer(user(3L));
        for (byte[] bad : List.of("<html>fake</html>".getBytes(), "<svg onload='alert(1)'/>".getBytes(), new byte[0]))
            mvc.perform(multipart("/media/images").file(new MockMultipartFile("file", "safe.png", "image/png", bad))
                    .header("Authorization", auth)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1801));
    }
    @Test void everyAdvertisedImageFormatCanBeDecodedAndStored() throws Exception {
        String auth = bearer(user(3L));
        for (String format : List.of("jpg", "png", "gif", "webp")) {
            var output = new ByteArrayOutputStream();
            assertTrue(ImageIO.write(new BufferedImage(12, 8, BufferedImage.TYPE_INT_RGB), format, output), format);
            String type = "image/" + (format.equals("jpg") ? "jpeg" : format);
            var result = mvc.perform(multipart("/media/images").file(new MockMultipartFile("file", "format." + format, type, output.toByteArray()))
                    .header("Authorization", auth)).andExpect(status().isOk()).andExpect(jsonPath("$.data.contentType").value(type)).andReturn();
            assertTrue(json.readTree(result.getResponse().getContentAsString()).at("/data/path").asText().endsWith("." + format));
        }
    }
    @Test void sizeAndPixelLimitsAreEnforcedBeforeDecode() throws Exception {
        String auth = bearer(user(3L));
        mvc.perform(multipart("/media/images").file(new MockMultipartFile("file", "huge.png", "image/png", new byte[10 * 1024 * 1024 + 1]))
                .header("Authorization", auth)).andExpect(status().isBadRequest());
        byte[] huge = png();
        java.nio.ByteBuffer.wrap(huge).putInt(16, 10000).putInt(20, 10000);
        mvc.perform(multipart("/media/images").file(new MockMultipartFile("file", "huge.png", "image/png", huge))
                .header("Authorization", auth)).andExpect(status().isBadRequest());
    }
    @Test void draftsAndUnreferencedImagesArePrivate() throws Exception {
        User owner = user(3L), stranger = user(3L); MediaAsset asset = upload(owner);
        reference(owner, asset, 0);
        mvc.perform(get("/" + asset.getPath())).andExpect(status().isNotFound());
        mvc.perform(get("/media/" + asset.getId() + "/file").header("Authorization", bearer(stranger))).andExpect(status().isNotFound());
        mvc.perform(get("/media/image").param("path", asset.getPath()).header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", "no-store")).andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
    @Test void publishedAndUnpublishedVisibilityTracksCurrentDatabase() throws Exception {
        User owner = user(3L); MediaAsset asset = upload(owner); Article article = reference(owner, asset, 1);
        mvc.perform(get("/" + asset.getPath())).andExpect(status().isOk());
        articles.update(Article.create().setId(article.getId()).setStatus(0));
        mvc.perform(get("/" + asset.getPath())).andExpect(status().isNotFound());
    }
    @Test void libraryAndDeletionAreIsolatedByOwner() throws Exception {
        User owner = user(3L), stranger = user(3L); MediaAsset asset = upload(owner);
        mvc.perform(get("/media").header("Authorization", bearer(stranger)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.records").isEmpty());
        mvc.perform(get("/media").param("all", "true").header("Authorization", bearer(stranger))).andExpect(status().isForbidden());
        mvc.perform(delete("/media/" + asset.getId()).header("Authorization", bearer(stranger))).andExpect(status().isNotFound());
        mvc.perform(get("/media").header("Authorization", bearer(owner))).andExpect(jsonPath("$.data.records[0].asset.id").value(asset.getId()));
    }
    @Test void articleReferencesBlockDeletion() throws Exception {
        User owner = user(3L); MediaAsset asset = upload(owner); Article article = reference(owner, asset, 0);
        mvc.perform(delete("/media/" + asset.getId()).header("Authorization", bearer(owner)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(1803));
        articles.update(Article.create().setId(article.getId()).setContent("图片引用已移除"));
        mvc.perform(delete("/media/" + asset.getId()).header("Authorization", bearer(owner))).andExpect(status().isOk());
        assertNull(assets.selectOneById(asset.getId()));
        mvc.perform(get("/media/" + asset.getId() + "/file").header("Authorization", bearer(owner))).andExpect(status().isNotFound());
    }
    @Test void avatarUsesTheSamePathAndCannotBeDeletedWhileActive() throws Exception {
        User owner = user(3L); String auth = bearer(owner);
        var result = mvc.perform(multipart("/users/upload/avatar").file(new MockMultipartFile("file", "avatar.png", "image/png", png()))
                .header("Authorization", auth)).andExpect(status().isOk()).andReturn();
        String path = json.readTree(result.getResponse().getContentAsString()).get("data").asText();
        assertTrue(path.startsWith("upload/avatar/" + owner.getNid() + "/"));
        assertEquals(path, users.selectOneById(owner.getId()).getAvatar());
        mvc.perform(get("/" + path)).andExpect(status().isOk());
        var list = mvc.perform(get("/media").param("business", "avatar").header("Authorization", auth)).andReturn();
        long id = json.readTree(list.getResponse().getContentAsString()).at("/data/records/0/asset/id").asLong();
        mvc.perform(delete("/media/" + id).header("Authorization", auth)).andExpect(status().isConflict());
    }
    @Test void privateImagesCannotBeAttachedToAnotherAuthorsDraft() throws Exception {
        User owner = user(3L), stranger = user(3L); MediaAsset asset = upload(owner);
        Category category = Category.create().setName("media_" + UUID.randomUUID().toString().substring(0, 8)).setDeleted(0);
        categories.insertSelective(category);
        mvc.perform(post("/articles/drafts").header("Authorization", bearer(stranger)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("title", "越权引用测试", "content", "![图](/" + asset.getPath() + ")", "categoryId", category.getId(), "tagIds", List.of()))))
                .andExpect(status().isNotFound());
    }
    @Test void arbitraryFilesAndTraversalAreNotServed() throws Exception {
        User owner = user(3L);
        mvc.perform(get("/media/image").param("path", "upload/article/../../application.yaml").header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/upload/arbitrary.txt")).andExpect(status().isNotFound());
        mvc.perform(get("/media").param("size", "1000").header("Authorization", bearer(owner))).andExpect(status().isBadRequest());
    }
    @Test void administrativeMediaAccessAlsoRequiresTheBusinessPermission() throws Exception {
        User owner = user(3L), admin = user(2L); MediaAsset asset = upload(owner);
        var principal = org.mockito.Mockito.mock(com.nebula.common.security.LoginUser.class);
        org.mockito.Mockito.when(principal.getUserId()).thenReturn(admin.getId());
        var context = org.springframework.security.core.context.SecurityContextHolder.getContext();
        context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
        assertThrows(com.nebula.common.exception.BusinessException.class, () -> media.requireFile(asset.getId()));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> media.list(1, 12, null, null, true));
        assertThrows(com.nebula.common.exception.BusinessException.class, () -> media.delete(asset.getId()));
        context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("article:list"))));
        assertEquals(asset.getId(), media.requireFile(asset.getId()).getId());
        assertThrows(com.nebula.common.exception.BusinessException.class, () -> media.delete(asset.getId()));
        assertNotNull(assets.selectOneById(asset.getId()));
    }
}
