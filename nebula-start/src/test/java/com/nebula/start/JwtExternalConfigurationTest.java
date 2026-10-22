package com.nebula.start;

import com.nebula.auth.service.TokenSessionService;
import com.nebula.auth.util.JwtUtil;
import com.nebula.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class JwtExternalConfigurationTest {
    @TempDir Path directory;

    private static String key() {
        return Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
    }

    private ApplicationContextRunner application() {
        return new ApplicationContextRunner()
                .withPropertyValues("spring.config.location=classpath:application.yaml",
                        "spring.config.import=optional:configtree:" + directory.toAbsolutePath().toString().replace('\\', '/') + "/")
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withBean(TokenSessionService.class, () -> mock(TokenSessionService.class))
                .withBean(JwtUtil.class);
    }

    private void verifyKey(JwtUtil jwt, String key) {
        String token = jwt.createToken(User.create().setId(42L).setPassword("hash"));
        assertEquals("42", Jwts.parser().verifyWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(key)))
                .build().parseSignedClaims(token).getPayload().getSubject());
    }

    @Test
    void actualApplicationYamlLoadsAnExternalSecretFile() throws Exception {
        String local = key();
        Files.writeString(directory.resolve("NEBULA_JWT_SECRET"), local);
        application().run(ctx -> {
            assertNull(ctx.getStartupFailure());
            verifyKey(ctx.getBean(JwtUtil.class), local);
        });
    }

    @Test
    void environmentSecretOverridesTheLocalFile() throws Exception {
        Files.writeString(directory.resolve("NEBULA_JWT_SECRET"), key());
        String external = key();
        application().withInitializer(ctx -> ctx.getEnvironment().getPropertySources().addFirst(
                new SystemEnvironmentPropertySource("isolated-jwt-environment", Map.of("NEBULA_JWT_SECRET", external))))
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    verifyKey(ctx.getBean(JwtUtil.class), external);
                });
    }

    @Test
    void emptyEnvironmentOverrideFailsClosedInsteadOfReusingLocalKey() throws Exception {
        Files.writeString(directory.resolve("NEBULA_JWT_SECRET"), key());
        application().withInitializer(ctx -> ctx.getEnvironment().getPropertySources().addFirst(
                new SystemEnvironmentPropertySource("isolated-jwt-environment", Map.of("NEBULA_JWT_SECRET", ""))))
                .run(ctx -> {
                    Throwable failure = ctx.getStartupFailure();
                    assertNotNull(failure);
                    while (failure.getCause() != null) failure = failure.getCause();
                    assertTrue(failure.getMessage().contains("signing key is missing"));
                });
    }
}
