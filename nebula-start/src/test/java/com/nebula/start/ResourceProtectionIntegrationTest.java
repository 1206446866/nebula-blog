package com.nebula.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.row.Db;
import com.nebula.auth.util.JwtUtil;
import com.nebula.common.exception.ResourceLimitException;
import com.nebula.common.security.*;
import com.nebula.media.entity.MediaAsset;
import com.nebula.media.service.*;
import com.nebula.user.entity.*;
import com.nebula.user.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 真实Redis和MySQL的低额度回归；只创建、清理专用账号、媒体和文件。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "nebula.publication.enabled=false", "nebula.resources.search.concurrency=1", "nebula.resources.decode-concurrency=1"})
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(ResourceProtectionIntegrationTest.TimeoutProbe.class)
class ResourceProtectionIntegrationTest extends JwtIntegrationTestSupport {
    static final String PREFIX = "test:resources:" + UUID.randomUUID() + ":";
    static final Path ROOT = Path.of("target/resource-tests-" + UUID.randomUUID()).toAbsolutePath().normalize();
    @DynamicPropertySource static void settings(DynamicPropertyRegistry registry) {
        registry.add("file.upload-root", ROOT::toString);
        registry.add("nebula.resources.key-prefix", () -> PREFIX);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserMapper users;
    @Autowired UserRoleMapper roles;
    @Autowired JwtUtil jwt;
    @Autowired StringRedisTemplate redis;
    @Autowired ResourceGuard guard;
    @Autowired ResourceLimitProperties rates;
    @Autowired MediaLimitProperties limits;
    @Autowired ImageStorage storage;
    @Autowired ThumbnailService thumbnails;
    @Autowired PlatformTransactionManager transactions;
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    final List<Long> accounts = new ArrayList<>();
    final List<String> tokens = new ArrayList<>();

    @BeforeEach void defaults() {
        TimeoutProbe.timeouts.clear();
        rates.getSearch().setAccount(2); rates.getSearch().setIp(3);
        rates.getUpload().setAccount(20); rates.getUpload().setIp(40);
        rates.getUpload().setAccountConcurrency(1); rates.getUpload().setIpConcurrency(2);
        rates.getRead().setAccount(240); rates.getRead().setIp(600);
        limits.setAccountBytes(500L*1024*1024); limits.setAccountFiles(2000);
        limits.setDailyBytes(200L*1024*1024); limits.setDailyFiles(200);
        limits.setMinFreeDiskBytes(0); limits.setThumbnailCacheBytes(16L*1024*1024); limits.setThumbnailCacheEntries(128);
    }
    @AfterEach void cleanup() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        tokens.forEach(jwt::revokeToken);
        for (Long id : accounts) {
            for (var row : Db.selectListBySql("SELECT path FROM media_asset WHERE user_id=?", id)) {
                String path = row.getString("path"); thumbnails.evict(path); Files.deleteIfExists(storage.resolve(path));
            }
            Db.deleteBySql("DELETE FROM article WHERE user_id=?", id);
            Db.deleteBySql("DELETE FROM media_asset WHERE user_id=?", id);
            Db.deleteBySql("DELETE FROM user_role WHERE user_id=?", id);
            Db.deleteBySql("DELETE FROM user WHERE id=?", id);
        }
        var keys = redis.keys(PREFIX + "*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
    }
    @AfterAll static void directories() throws Exception {
        assertTrue(ROOT.getFileName().toString().startsWith("resource-tests-"));
        if (Files.exists(ROOT)) try (var paths = Files.walk(ROOT)) {
            for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
    private User user() {
        var user = User.create().setNid("resource_"+UUID.randomUUID().toString().replace("-", ""))
                .setUsername("资源防护回归").setPassword("unused-fixture-hash").setStatus(0).setDeleted(0);
        users.insertSelective(user); accounts.add(user.getId());
        roles.insert(UserRole.create().setUserId(user.getId()).setRoleId(3L));
        return user;
    }
    private String token(User user) { String token = jwt.createToken(user); tokens.add(token); return token; }
    private static byte[] image() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(12,8,BufferedImage.TYPE_INT_ARGB),"png",out); return out.toByteArray();
    }
    private MvcResult upload(String token, byte[] image) throws Exception {
        return mvc.perform(multipart("/media/images").file(new MockMultipartFile("file","fixture.png","image/png",image))
                .header("Authorization","Bearer "+token)).andReturn();
    }
    private MediaAsset asset(MvcResult result) throws Exception {
        assertEquals(200,result.getResponse().getStatus(),result.getResponse().getContentAsString());
        return json.treeToValue(json.readTree(result.getResponse().getContentAsString()).path("data"),MediaAsset.class);
    }
    private void error(MvcResult result,int status,int code) throws Exception {
        assertEquals(status,result.getResponse().getStatus(),result.getResponse().getContentAsString());
        assertEquals(code,json.readTree(result.getResponse().getContentAsString()).path("code").asInt());
    }
    private long active(User user) { return Db.selectOneBySql("SELECT COUNT(*) n FROM media_asset WHERE user_id=? AND deleted=0",user.getId()).getLong("n"); }

    @Test void searchBudgetFollowsAccountWhenIpChangesAndExposesRetryAfter() throws Exception {
        String auth="Bearer "+token(user());
        for (int i=1;i<=2;i++) {
            String address="192.0.2."+i;
            mvc.perform(get("/search").header("Authorization",auth).with(r->{r.setRemoteAddr(address);return r;})).andExpect(status().isOk());
        }
        mvc.perform(get("/search").header("Authorization",auth).header("Origin","http://localhost:5173").with(r->{r.setRemoteAddr("192.0.2.10");return r;}))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value(429))
                .andExpect(header().exists("Retry-After")).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(header().string("Access-Control-Expose-Headers",org.hamcrest.Matchers.containsString("Retry-After")));
        mvc.perform(get("/search").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
    }
    @Test void anonymousImagesShareTrustedIpBudgetDespiteSpoofedForwardingHeaders() throws Exception {
        rates.getRead().setIp(3);
        for (int i=0;i<3;i++) mvc.perform(get("/upload/nonexistent.png").header("X-Forwarded-For","192.0.2."+i)).andExpect(status().isNotFound());
        mvc.perform(get("/upload/nonexistent.png").header("X-Forwarded-For","198.51.100.2"))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
    }
    @Test void rotatingAccountsCannotBypassTheSearchIpBudget() throws Exception {
        rates.getSearch().setAccount(10);
        for(int i=0;i<3;i++) mvc.perform(get("/search").header("Authorization","Bearer "+token(user()))).andExpect(status().isOk());
        mvc.perform(get("/search").header("Authorization","Bearer "+token(user()))).andExpect(status().isTooManyRequests());
    }
    @Test void realHttpUploadIsLimitedBeforeMultipartParsing() throws Exception {
        rates.getUpload().setAccount(1); String auth=token(user()); asset(upload(auth,image()));
        var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/media/images"))
                .timeout(java.time.Duration.ofSeconds(10)).header("Authorization","Bearer "+auth)
                .header("Content-Type","multipart/form-data; boundary=deliberately-missing")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("not a valid multipart body")).build();
        var response=java.net.http.HttpClient.newHttpClient().send(request,java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(429,response.statusCode(),response.body());
        assertTrue(response.headers().firstValue("Retry-After").isPresent());
    }
    @Test void searchStatementsReceiveTheTransactionDeadline() throws Exception {
        mvc.perform(get("/search").param("q","resource-fixture-no-match").header("Authorization","Bearer "+token(user())))
                .andExpect(status().isOk());
        assertFalse(TimeoutProbe.timeouts.isEmpty(),"必须实际检查搜索SQL的JDBC超时设置");
        assertTrue(TimeoutProbe.timeouts.stream().allMatch(seconds->seconds>0 && seconds<=3),TimeoutProbe.timeouts.toString());
    }
    @Test void avatarAndArticleUploadsShareOneFrequencyBudget() throws Exception {
        rates.getUpload().setAccount(2);
        String auth=token(user()); byte[] image=image(); asset(upload(auth,image));
        mvc.perform(multipart("/users/upload/avatar").file(new MockMultipartFile("file","avatar.png","image/png",image))
                .header("Authorization","Bearer "+auth)).andExpect(status().isOk());
        error(upload(auth,image),429,429);
    }
    @Test void accountBytesAreEnforcedAndDeletionReturnsAvailableSpace() throws Exception {
        byte[] image=image(); limits.setAccountBytes(image.length);
        User user=user(); String auth=token(user); var first=asset(upload(auth,image));
        error(upload(auth,image),409,1805); assertEquals(1,active(user));
        mvc.perform(delete("/media/"+first.getId()).header("Authorization","Bearer "+auth)).andExpect(status().isOk());
        assertFalse(Files.exists(storage.resolve(first.getPath())));
        asset(upload(auth,image)); assertEquals(1,active(user));
    }
    @Test void accountFileCountAlsoLimitsTinyImages() throws Exception {
        limits.setAccountFiles(1); String auth=token(user()); asset(upload(auth,image())); error(upload(auth,image()),409,1805);
    }
    @Test void deletingImagesDoesNotResetDailyUploadCount() throws Exception {
        limits.setDailyFiles(1); User owner=user(); String auth=token(owner); var first=asset(upload(auth,image()));
        mvc.perform(delete("/media/"+first.getId()).header("Authorization","Bearer "+auth)).andExpect(status().isOk());
        error(upload(auth,image()),409,1806); assertEquals(0,active(owner));
    }
    @Test void deletingImagesDoesNotResetDailyUploadBytes() throws Exception {
        byte[] image=image(); limits.setDailyBytes(image.length); String auth=token(user()); var first=asset(upload(auth,image));
        mvc.perform(delete("/media/"+first.getId()).header("Authorization","Bearer "+auth)).andExpect(status().isOk());
        error(upload(auth,image),409,1806);
    }
    @Test void invalidAndRolledBackUploadsConsumeNoPersistentQuota() throws Exception {
        limits.setDailyFiles(1); User owner=user(); String auth=token(owner);
        error(upload(auth,new byte[]{1,2,3}),400,1801);
        var rolledBack=new AtomicReference<MediaAsset>();
        new TransactionTemplate(transactions).execute(status->{
            try { rolledBack.set(asset(upload(auth,image()))); } catch(Exception e){throw new RuntimeException(e);}
            status.setRollbackOnly(); return null;
        });
        assertEquals(0,active(owner)); assertFalse(Files.exists(storage.resolve(rolledBack.get().getPath())));
        asset(upload(auth,image()));
    }
    @Test void lowDiskStopsBeforeWritingAndDoesNotLeakPermitsOrQuota() throws Exception {
        User owner=user(); String auth=token(owner); limits.setMinFreeDiskBytes(Long.MAX_VALUE);
        error(upload(auth,image()),507,1807); assertEquals(0,active(owner));
        limits.setMinFreeDiskBytes(0); asset(upload(auth,image()));
    }
    @Test void decodeAndSearchSaturationRejectWorkThenRecover() throws Exception {
        String auth=token(user());
        try (var permit=guard.acquire(ResourceGuard.Work.DECODE)) { error(upload(auth,image()),429,429); }
        asset(upload(auth,image()));
        try (var permit=guard.acquire(ResourceGuard.Work.SEARCH)) {
            mvc.perform(get("/search").header("Authorization","Bearer "+auth)).andExpect(status().isTooManyRequests());
        }
        mvc.perform(get("/search").header("Authorization","Bearer "+auth)).andExpect(status().isOk());
    }
    @Test void cachedThumbnailAlwaysRechecksCurrentPrivateAndPublishedVisibility() throws Exception {
        User owner=user(),stranger=user(); String auth=token(owner),other=token(stranger); var item=asset(upload(auth,image()));
        String url="/media/"+item.getId()+"/file";
        mvc.perform(get(url).param("thumbnail","true").header("Authorization","Bearer "+auth)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(get(url).param("thumbnail","true").header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
        Db.insertBySql("INSERT INTO article(title,content,author,user_id,status,deleted) VALUES(?,?,?,?,1,0)","资源可见性回归","![图](/"+item.getPath()+")",owner.getUsername(),owner.getId());
        mvc.perform(get(url).param("thumbnail","true").header("Authorization","Bearer "+other)).andExpect(status().isOk());
        Db.updateBySql("UPDATE article SET status=0 WHERE user_id=?",owner.getId());
        mvc.perform(get(url).param("thumbnail","true").header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
        mvc.perform(get("/"+item.getPath())).andExpect(status().isNotFound());
    }
    @Test void thumbnailCacheEvictsOldEntriesAndDoesNotCacheOversizedEntries() throws Exception {
        limits.setThumbnailCacheEntries(1); String auth=token(user());
        var first=asset(upload(auth,image()));
        var second=asset(upload(auth,image()));
        thumbnails.render(first); thumbnails.render(second);
        try(var permit=guard.acquire(ResourceGuard.Work.DECODE)) {
            assertNotNull(thumbnails.render(second));
            assertThrows(ResourceLimitException.class,()->thumbnails.render(first));
        }
        thumbnails.evict(second.getPath()); limits.setThumbnailCacheBytes(1); thumbnails.render(second);
        try(var permit=guard.acquire(ResourceGuard.Work.DECODE)) { assertThrows(ResourceLimitException.class,()->thumbnails.render(second)); }
    }
    @Test void simultaneousUploadsCannotExceedOneAccountSlot() throws Exception {
        rates.getUpload().setAccountConcurrency(2);
        limits.setAccountFiles(1); String auth=token(user()); byte[] image=image();
        var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        try {
            List<Future<Integer>> results=new ArrayList<>();
            for(int i=0;i<2;i++) results.add(pool.submit(()->{start.await();return upload(auth,image).getResponse().getStatus();}));
            start.countDown(); var statuses=new ArrayList<Integer>(); for(var result:results) statuses.add(result.get(15,TimeUnit.SECONDS));
            Collections.sort(statuses); assertEquals(List.of(200,409),statuses);
        } finally {pool.shutdownNow();}
    }
    @Test void redisBudgetIsAtomicForConcurrentRequests() throws Exception {
        var configuration=new ResourceLimitProperties(); configuration.setKeyPrefix(PREFIX+"parallel:");
        configuration.getSearch().setAccount(2); configuration.getSearch().setIp(20); configuration.getSearch().setConcurrency(20);
        configuration.getSearch().setAccountConcurrency(20); configuration.getSearch().setIpConcurrency(20);
        var parallel=new ResourceGuard(redis,configuration); var pool=Executors.newFixedThreadPool(12); var start=new CountDownLatch(1);
        try {
            List<Future<Boolean>> results=new ArrayList<>();
            for(int i=0;i<12;i++) results.add(pool.submit(()->{start.await();try(var permit=parallel.enter(ResourceGuard.Work.SEARCH,"192.0.2.5",99L)){return true;}catch(ResourceLimitException e){assertEquals(429,e.getHttpStatus());return false;}}));
            start.countDown(); int accepted=0; for(var result:results) if(result.get(10,TimeUnit.SECONDS)) accepted++;
            assertEquals(2,accepted);
        } finally {pool.shutdownNow();}
    }
    @Test void accountConcurrencyCannotBeBypassedBySwitchingIpAndReleasesAfterFailure() throws Exception {
        User owner=user(); String auth=token(owner);
        try(var first=guard.enter(ResourceGuard.Work.UPLOAD,"192.0.2.10",owner.getId())) {
            error(upload(auth,image()),429,429);
        }
        asset(upload(auth,image()));
    }
    @Test void oneAnonymousSourceCannotOccupyAllImageReadSlots() {
        rates.getRead().setIpConcurrency(1);
        try(var first=guard.enter(ResourceGuard.Work.READ,"192.0.2.10",null)) {
            assertThrows(ResourceLimitException.class,()->guard.enter(ResourceGuard.Work.READ,"192.0.2.10",null));
            try(var other=guard.enter(ResourceGuard.Work.READ,"192.0.2.11",null)) { assertNotNull(other); }
        }
        try(var recovered=guard.enter(ResourceGuard.Work.READ,"192.0.2.10",null)) { assertNotNull(recovered); }
        rates.getRead().setIpConcurrency(8);
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class TimeoutProbe {
        static final Queue<Integer> timeouts=new ConcurrentLinkedQueue<>();
        @org.springframework.context.annotation.Bean
        org.apache.ibatis.plugin.Interceptor queryDeadlineObserver() { return new DeadlineObserver(); }
    }
    @org.apache.ibatis.plugin.Intercepts(@org.apache.ibatis.plugin.Signature(
            type=org.apache.ibatis.executor.statement.StatementHandler.class,method="prepare",args={java.sql.Connection.class,Integer.class}))
    static class DeadlineObserver implements org.apache.ibatis.plugin.Interceptor {
        @Override public Object intercept(org.apache.ibatis.plugin.Invocation invocation) throws Throwable {
            var handler=(org.apache.ibatis.executor.statement.StatementHandler)invocation.getTarget();
            Object statement=invocation.proceed();
            if(handler.getBoundSql().getSql().contains(") hits")) TimeoutProbe.timeouts.add(((java.sql.Statement)statement).getQueryTimeout());
            return statement;
        }
    }
}
