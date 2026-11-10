package com.nebula.start;

import com.mybatisflex.core.row.Db;
import com.nebula.auth.util.JwtUtil;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import com.nebula.common.security.DatabaseReadBudget;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** 显式启用的真实HTTP验收：只使用既有隔离库和Redis第13分库，不操作开发业务数据。 */
@EnabledIfSystemProperty(named = "nebula.test.request-budgets", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/nebula_es_20261108?serverTimezone=Asia/Shanghai",
        "spring.data.redis.database=13", "nebula.publication.enabled=false",
        "nebula.search.synchronization-enabled=false", "nebula.search.alias=nebula-search-budget-it",
        "nebula.resources.key-prefix=nebula:request-budget-it:",
        "nebula.security.request.json-read-timeout-ms=1500", "server.tomcat.connection-timeout=1s"
})
@Import(RequestBudgetHttpIntegrationTest.Probe.class)
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class RequestBudgetHttpIntegrationTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) throws Exception {
        var properties = new Properties();
        try (var input = Files.newInputStream(Path.of("target/es-client-20261108/.private/database.properties"))) {
            properties.load(input);
        }
        registry.add("spring.datasource.username", () -> "nebula_es_20261108");
        registry.add("spring.datasource.password", () -> properties.getProperty("password"));
    }

    @LocalServerPort int port;
    @Autowired HikariDataSource pool;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean UserMapper users;
    @Autowired JwtUtil jwt;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
    final List<String> tokens = new ArrayList<>();
    User user;
    String token;
    static final String PASSWORD = "Budget-test-20261108";
    final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    @BeforeEach void setup() {
        assertEquals("nebula_es_20261108", Db.selectOneBySql("SELECT DATABASE() n").getString("n"));
        user = User.create().setNid("budget_" + UUID.randomUUID().toString().substring(0, 12))
                .setUsername("请求预算验收").setPassword(passwords.encode(PASSWORD)).setStatus(0).setDeleted(0);
        users.insert(user); token = jwt.createToken(user); tokens.add(token);
    }

    @AfterEach void cleanup() {
        tokens.forEach(jwt::revokeToken);
        if (user != null && user.getId() != null) {
            Db.deleteBySql("DELETE FROM login_log WHERE user_id=?", user.getId());
            Db.deleteBySql("DELETE FROM user WHERE id=? AND nid=?", user.getId(), user.getNid());
            Db.deleteBySql("DELETE FROM search_sync_task WHERE kind='user' AND target_id=?", user.getId());
        }
        assertNull(DatabaseReadBudget.remainingSeconds(), "请求预算不能泄漏到其它线程");
        assertEquals(0, pool.getHikariPoolMXBean().getActiveConnections());
    }

    private HttpResponse<String> request(String path, String body, String bearer) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(12));
        if (bearer != null) builder.header("Authorization", "Bearer " + bearer);
        if (body == null) builder.GET(); else builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void busy(HttpResponse<String> response) {
        assertEquals(503, response.statusCode(), response.body());
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(""));
        assertTrue(response.headers().firstValue("Retry-After").isPresent());
        assertFalse(response.body().contains("jdbc:"));
    }

    @Test void exhaustedPoolRejectsAuthenticationAndLoginPromptlyThenRecovers() throws Exception {
        assertEquals(1000, pool.getConnectionTimeout());
        var holders = new ArrayList<java.sql.Connection>();
        try {
            for (int i = 0; i < pool.getMaximumPoolSize(); i++) holders.add(pool.getConnection());
            for (String path : List.of("/search?q=budget", "/api/profile/" + user.getId(), "/__budget/write")) {
                long start = System.nanoTime();
                busy(request(path, path.endsWith("write") ? "{}" : null, token));
                System.out.println("POOL_WAIT_MS " + path + " " + (System.nanoTime() - start) / 1_000_000);
                assertTrue((System.nanoTime() - start) / 1_000_000 < 2500, "池满不能等待十余秒");
            }
            busy(request("/auth/login", "{\"account\":\"" + user.getNid() + "\",\"password\":\"" + PASSWORD + "\"}", null));
        } finally { for (var holder : holders) holder.close(); }
        assertEquals(200, request("/api/profile/" + user.getId(), null, token).statusCode());
        assertEquals(200, request("/__budget/write", "{}", token).statusCode());
    }

    @Test void realSqlTimeoutReturnsBusyAndConnectionCanBeReused() throws Exception {
        long start = System.nanoTime();
        busy(request("/__budget/slow", null, token));
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println("SQL_TIMEOUT_MS " + ms);
        assertTrue(ms >= 800 && ms < 3500, "实际SQL必须在预算内被取消，耗时=" + ms);
        assertEquals(200, request("/__budget/read", null, token).statusCode());
    }

    @Test void successfulLoginWriteAndRollbackRemainUsable() throws Exception {
        var login = request("/auth/login", "{\"account\":\"" + user.getNid() + "\",\"password\":\"" + PASSWORD + "\"}", null);
        assertEquals(200, login.statusCode(), login.body());
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(login.body());
        String issued = json.path("data").path("token").asText();
        assertFalse(issued.isBlank()); tokens.add(issued);
        assertEquals(200, request("/__budget/write", "{}", issued).statusCode());
        assertEquals(500, request("/__budget/rollback", "{}", issued).statusCode());
        assertEquals("请求预算验收", users.selectOneById(user.getId()).getUsername());
        assertEquals(200, request("/__budget/read", null, issued).statusCode());
    }

    @Test void chunkedOversizeAndMalformedJsonAreRejectedAndNextRequestWorks() throws Exception {
        org.mockito.Mockito.clearInvocations(users);
        try (var socket = socket()) {
            send(socket, headers("/auth/login", "Transfer-Encoding: chunked\r\n") + "2001\r\n" + " ".repeat(8193) + "\r\n0\r\n\r\n");
            assertTrue(receive(socket).contains(" 413 "));
        }
        assertEquals(413, request("/auth/login", " ".repeat(8193), token).statusCode());
        assertEquals(400, request("/auth/login", "{", null).statusCode());
        org.mockito.Mockito.verify(users, org.mockito.Mockito.never()).selectOneByCondition(org.mockito.ArgumentMatchers.any());
        assertEquals(200, request("/__budget/read", null, token).statusCode());
    }

    @Test void idleBodyTimesOutAndContinuousTrickleCannotKeepItsSlotForever() throws Exception {
        org.mockito.Mockito.clearInvocations(users);
        try (var socket = socket()) {
            send(socket, headers("/auth/login", "Content-Length: 100\r\n") + "{");
            long start = System.nanoTime();
            String idleResponse = receive(socket);
            assertTrue(idleResponse.contains(" 408 "), idleResponse);
            assertTrue(idleResponse.contains("\"code\":408"), idleResponse);
            assertFalse(idleResponse.contains("SocketTimeoutException"));
            assertTrue((System.nanoTime() - start) / 1_000_000 < 3000);
        }
        try (var socket = socket()) {
            send(socket, headers("/auth/login", "Transfer-Encoding: chunked\r\n"));
            var executor = Executors.newSingleThreadExecutor();
            try {
                var response = executor.submit(() -> receive(socket));
                long start = System.nanoTime();
                while (!response.isDone() && (System.nanoTime() - start) < 4_000_000_000L) {
                    try { send(socket, "1\r\n \r\n"); } catch (java.io.IOException closed) { break; }
                    Thread.sleep(100);
                }
                assertTrue(response.get(2, TimeUnit.SECONDS).contains(" 408 "));
                assertTrue((System.nanoTime() - start) / 1_000_000 < 3500);
            } finally { executor.shutdownNow(); }
        }
        org.mockito.Mockito.verify(users, org.mockito.Mockito.never()).selectOneByCondition(org.mockito.ArgumentMatchers.any());
        assertEquals(200, request("/__budget/read", null, token).statusCode());
    }

    @Test void disconnectedBodiesReleaseEveryAdmissionSlot() throws Exception {
        for (int i = 0; i < 40; i++) {
            try (var socket = socket()) {
                send(socket, headers("/auth/login", "Content-Length: 100\r\n") + "{");
                socket.setSoLinger(true, 0);
            }
        }
        Thread.sleep(300);
        assertEquals(200, request("/__budget/read", null, token).statusCode());
    }

    @Test void exactJsonLimitAndLongUnicodeContentSurviveRealHttp() throws Exception {
        String prefix = "{\"content\":\"", suffix = "\"}";
        String content = "a".repeat(2 * 1024 * 1024 - prefix.length() - suffix.length());
        var exact = request("/__budget/body", prefix + content + suffix, token);
        assertEquals(200, exact.statusCode(), exact.body());
        assertEquals(Integer.toString(content.length()), exact.body());
        assertEquals(413, request("/__budget/body", prefix + content + "a" + suffix, token).statusCode());
        String unicode = "星".repeat(199998) + "🌌";
        var longBody = request("/__budget/body", prefix + unicode + suffix, token);
        assertEquals(200, longBody.statusCode(), longBody.body());
        assertEquals(Integer.toString(unicode.length()), longBody.body());
    }

    @Test void containerErrorPermissionDoesNotOpenDirectAnonymousAccess() throws Exception {
        assertEquals(401, request("/error", null, null).statusCode());
        assertEquals(401, request("/__budget/read", null, null).statusCode());
        assertEquals(404, request("/nonexistent-budget-route", null, token).statusCode());
    }

    private Socket socket() throws Exception { var socket = new Socket("127.0.0.1", port); socket.setSoTimeout(6000); return socket; }
    private String headers(String path, String extra) { return "POST " + path + " HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\nAuthorization: Bearer " + token + "\r\n" + extra + "Connection: close\r\n\r\n"; }
    private void send(Socket socket, String data) throws Exception { socket.getOutputStream().write(data.getBytes(StandardCharsets.UTF_8)); socket.getOutputStream().flush(); }
    private String receive(Socket socket) throws Exception { return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8); }

    /** 探针只访问验收账号；写入使用真实事务，异常分支确认回滚。 */
    @RestController
    static class Probe {
        @PostMapping("/__budget/body") public int body(@RequestBody Map<String, String> body) { return body.get("content").length(); }
        @GetMapping("/__budget/read") public Object read() { return Db.selectOneBySql("SELECT 1 n"); }
        @GetMapping("/__budget/slow") public Object slow() {
            try (var budget = DatabaseReadBudget.open(1)) { return Db.selectOneBySql("SELECT SLEEP(4) n"); }
        }
        @PostMapping("/__budget/write") @Transactional public Object write() {
            assertNull(DatabaseReadBudget.remainingSeconds(), "认证读取期限不能影响后续写事务");
            return Db.selectOneBySql("SELECT 1 n FOR UPDATE");
        }
        @PostMapping("/__budget/rollback") @Transactional public Object rollback() {
            Db.updateBySql("UPDATE user SET username='应回滚' WHERE id=?", com.nebula.common.util.SecurityUtils.getUserId());
            throw new IllegalStateException("隔离事务回滚探针");
        }
    }
}
