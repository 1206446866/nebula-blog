package com.nebula.start;

import com.nebula.auth.util.JwtUtil;
import com.nebula.common.exception.AuthException;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.result.Result;
import com.nebula.user.entity.User;
import com.nebula.user.mapper.UserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.nebula.common.constant.LoginStatus.PASSWORD_ERROR;
import static com.nebula.common.exception.code.ArticleErrorCode.*;
import static com.nebula.common.exception.code.AuthErrorCode.USER_NOT_FOUND;
import static com.nebula.common.exception.code.CommentErrorCode.COMMENT_ACCESS_DENIED;
import static com.nebula.common.exception.code.RegistrationErrorCode.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Complete CORS/Security/MVC chain, real Flex users and Redis sessions; database writes roll back. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(HttpBoundaryIntegrationTest.ProbeController.class)
@ExtendWith(OutputCaptureExtension.class)
class HttpBoundaryIntegrationTest extends JwtIntegrationTestSupport {
    private static final String ORIGIN = "http://localhost:5173";
    private static final String PRIVATE = "private-internal-table-and-class-marker";
    @Autowired MockMvc mvc;
    @Autowired UserMapper users;
    @MockitoSpyBean JwtUtil jwt;
    private final List<String> tokens = new ArrayList<>();

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        tokens.forEach(jwt::revokeToken);
    }

    private String bearer() {
        var user = User.create().setNid("http_" + UUID.randomUUID()).setUsername("HTTP boundary fixture")
                .setPassword("unused-http-fixture-hash").setStatus(0).setDeleted(0);
        assertEquals(1, users.insert(user));
        String token = jwt.createToken(user);
        tokens.add(token);
        return "Bearer " + token;
    }

    @ParameterizedTest
    @CsvSource({"http://localhost:5173,POST", "http://localhost:5173,PUT",
            "http://127.0.0.1:5173,POST", "http://127.0.0.1:5173,DELETE"})
    void allowedPreflightsRunBeforeAuthentication(String origin, String method) throws Exception {
        mvc.perform(options("/users/1").header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-even-if-sent"))
                .andExpect(status().isOk()).andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString(method)))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("authorization")))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        verify(jwt, never()).parseToken(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://untrusted.example.invalid", "http://localhost:5173.evil.invalid", "null", "http://localhost:3000"})
    void unlistedOriginsAreRejected(String origin) throws Exception {
        mvc.perform(options("/auth/login").header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        mvc.perform(post("/auth/login").header(HttpHeaders.ORIGIN, origin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"must-not-login\",\"password\":\"not-used\"}"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void unsupportedMethodsAndHeadersAreNotReflected() throws Exception {
        mvc.perform(options("/users/1").header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "TRACE"))
                .andExpect(status().isForbidden());
        mvc.perform(options("/users/1").header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Internal-Access"))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualAndNonPreflightRequestsStillRequireAuthentication() throws Exception {
        mvc.perform(get("/users/1").header(HttpHeaders.ORIGIN, ORIGIN))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN));
        mvc.perform(options("/users/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/users/1").header(HttpHeaders.ORIGIN, ORIGIN).header(HttpHeaders.AUTHORIZATION, "Bearer malformed"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN));
    }

    @Test
    void methodSecurityReturnsTheSamePublicForbiddenResponseAsTheFilterHandler() throws Exception {
        String auth = bearer();
        mvc.perform(get("/articles").header(HttpHeaders.ORIGIN, ORIGIN).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("权限不足"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN));
        mvc.perform(get("/_test/http/permission").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("权限不足"));
    }

    @ParameterizedTest
    @CsvSource({"article-missing,404,40401", "article-denied,403,40407", "comment-denied,403,5002",
            "email-conflict,409,40910", "captcha,400,40010", "user-missing,404,40101", "article-write,500,40403"})
    void businessCodesArePreservedButDoNotDetermineHttpStatusByTheirDigits(String kind, int status, int code) throws Exception {
        mvc.perform(get("/_test/http/error/" + kind).header(HttpHeaders.AUTHORIZATION, bearer()).header(HttpHeaders.ORIGIN, ORIGIN))
                .andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.data").isEmpty()).andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
                .andExpect(content().string(not(containsString(PRIVATE))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"runtime", "checked", "database"})
    void unexpectedErrorsDoNotExposeImplementationDetailsButAreLogged(String kind, CapturedOutput output) throws Exception {
        mvc.perform(get("/_test/http/error/" + kind).header(HttpHeaders.AUTHORIZATION, bearer()).header(HttpHeaders.ORIGIN, ORIGIN))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务器异常，请稍后重试"))
                .andExpect(jsonPath("$.data").isEmpty()).andExpect(content().string(not(containsString(PRIVATE))))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN));
        assertTrue(output.getAll().contains(PRIVATE), "Detailed cause must remain available in server logs");
    }

    @Test
    void databaseConflictsUseAStablePublicMessage() throws Exception {
        mvc.perform(get("/_test/http/error/conflict").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("数据冲突，请检查后重试"))
                .andExpect(content().string(not(containsString(PRIVATE))));
    }

    @ParameterizedTest
    @CsvSource({"denied,403,403", "authentication,401,401", "login,401,3", "oversized,413,413"})
    void frameworkAndLoginFailuresHaveRealHttpStatuses(String kind, int status, int code) throws Exception {
        mvc.perform(get("/_test/http/error/" + kind).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code))
                .andExpect(content().string(not(containsString(PRIVATE))));
    }

    @Test
    void malformedAndMissingParametersDoNotEchoValuesOrInternalTypes() throws Exception {
        String auth = bearer();
        mvc.perform(get("/_test/http/number").param("value", PRIVATE).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400))
                .andExpect(content().string(not(containsString(PRIVATE))));
        mvc.perform(get("/_test/http/number").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/_test/http/number").param("value", "0").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void invalidJsonAndValidationReturnClientErrors() throws Exception {
        String auth = bearer();
        mvc.perform(post("/_test/http/validated").header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":" + PRIVATE))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400))
                .andExpect(content().string(not(containsString(PRIVATE))));
        mvc.perform(post("/_test/http/validated").header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("请输入标题"));
    }

    @Test
    void queryObjectBindingDoesNotExposeRejectedValuesOrConversionClasses() throws Exception {
        mvc.perform(get("/_test/http/page").param("page", PRIVATE).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求格式或参数类型错误"))
                .andExpect(content().string(not(containsString(PRIVATE))))
                .andExpect(content().string(not(containsString("java.lang"))));
    }

    @Test
    void unsupportedMethodMediaTypeAndUnknownRouteAreNotServerErrors() throws Exception {
        String auth = bearer();
        mvc.perform(post("/_test/http/number").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").value(405))
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")));
        mvc.perform(post("/_test/http/validated").header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.TEXT_PLAIN).content(PRIVATE))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value(415));
        mvc.perform(get("/_test/http/does-not-exist").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("请求的资源不存在"));
    }

    @RestController
    @RequestMapping("/_test/http")
    public static class ProbeController {
        @GetMapping("/error/{kind}")
        public Result<Void> error(@PathVariable String kind) throws Exception {
            throw switch (kind) {
                case "article-missing" -> new BusinessException(ARTICLE_NOT_FOUND);
                case "article-denied" -> new BusinessException(ARTICLE_ACCESS_DENIED);
                case "comment-denied" -> new BusinessException(COMMENT_ACCESS_DENIED);
                case "email-conflict" -> new BusinessException(EMAIL_ALREADY_REGISTERED);
                case "captcha" -> new BusinessException(CAPTCHA_INVALID);
                case "user-missing" -> new BusinessException(USER_NOT_FOUND);
                case "article-write" -> new BusinessException(ARTICLE_CREATE_FAILED);
                case "checked" -> new Exception(PRIVATE);
                case "database" -> new DataAccessResourceFailureException("SELECT password FROM " + PRIVATE);
                case "conflict" -> new DataIntegrityViolationException("Duplicate index " + PRIVATE);
                case "denied" -> new AccessDeniedException(PRIVATE);
                case "authentication" -> new BadCredentialsException(PRIVATE);
                case "login" -> new AuthException(PASSWORD_ERROR);
                case "oversized" -> new MaxUploadSizeExceededException(10);
                default -> new IllegalStateException(PRIVATE);
            };
        }

        @GetMapping("/permission")
        @PreAuthorize("hasAuthority('test:never')")
        public Result<Void> permission() { return Result.success(null); }

        @GetMapping("/number")
        public Result<Integer> number(@RequestParam @Min(1) int value) { return Result.success(value); }

        @PostMapping(value = "/validated", consumes = MediaType.APPLICATION_JSON_VALUE)
        public Result<String> validated(@Valid @RequestBody Input input) { return Result.success(input.title()); }

        @GetMapping("/page")
        public Result<Integer> page(@Valid @ModelAttribute PageQuery query) { return Result.success(query.page()); }
    }

    public record Input(@NotBlank(message = "请输入标题") String title) { }
    public record PageQuery(@Min(1) int page) { }
}
