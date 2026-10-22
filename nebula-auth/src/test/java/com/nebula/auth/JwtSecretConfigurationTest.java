package com.nebula.auth;

import com.nebula.auth.service.TokenSessionService;
import com.nebula.auth.util.JwtUtil;
import com.nebula.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtSecretConfigurationTest {
    private final TokenSessionService sessions = mock(TokenSessionService.class);

    private static String randomKey(int bytes) {
        byte[] key = new byte[bytes];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    private ApplicationContextRunner context() {
        return new ApplicationContextRunner().withBean(TokenSessionService.class, () -> sessions)
                .withBean(JwtUtil.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void missingOrBlankKeysPreventStartup(String secret) {
        var runner = secret == null ? context() : context().withPropertyValues("jwt.secret=" + secret);
        runner.run(ctx -> {
            assertNotNull(ctx.getStartupFailure());
            assertTrue(rootCause(ctx.getStartupFailure()).getMessage().contains("signing key is missing"));
            verifyNoInteractions(sessions);
        });
    }

    @Test
    void invalidBase64IsRejectedWithoutLoggingTheConfiguredValue() {
        String secret = "secret-value-must-not-appear!";
        context().withPropertyValues("jwt.secret=" + secret).run(ctx -> {
            assertNotNull(ctx.getStartupFailure());
            var trace = new StringWriter();
            ctx.getStartupFailure().printStackTrace(new PrintWriter(trace));
            assertFalse(trace.toString().contains(secret));
            assertTrue(rootCause(ctx.getStartupFailure()).getMessage().contains("Base64"));
            assertNull(rootCause(ctx.getStartupFailure()).getCause());
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 16, 31})
    void encodedLengthDoesNotDisguiseAWeakDecodedKey(int bytes) {
        String secret = randomKey(bytes);
        context().withPropertyValues("jwt.secret=" + secret).run(ctx -> {
            assertNotNull(ctx.getStartupFailure());
            assertTrue(rootCause(ctx.getStartupFailure()).getMessage().contains("at least 32"));
            assertFalse(rootCause(ctx.getStartupFailure()).getMessage().contains(secret));
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {32, 48, 64})
    void strongBase64KeysProduceHs256TokensVerifiableWithDecodedBytes(int bytes) {
        String secret = randomKey(bytes);
        context().withPropertyValues("jwt.secret=" + secret, "jwt.expire=60000").run(ctx -> {
            assertNull(ctx.getStartupFailure());
            JwtUtil jwt = ctx.getBean(JwtUtil.class);
            User user = User.create().setId(42L).setPassword("stored-password-hash");
            String token = jwt.createToken(user);
            var parsed = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
                    .build().parseSignedClaims(token);
            assertEquals("HS256", parsed.getHeader().getAlgorithm());
            assertEquals("42", parsed.getPayload().getSubject());
            assertNotNull(parsed.getPayload().getId());
            assertTrue(jwt.validateToken(token));
            verify(sessions).register(eq(parsed.getPayload().getId()), same(user), any());
        });
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void nonPositiveLifetimePreventsStartup(long expire) {
        context().withPropertyValues("jwt.secret=" + randomKey(32), "jwt.expire=" + expire).run(ctx -> {
            assertNotNull(ctx.getStartupFailure());
            assertTrue(rootCause(ctx.getStartupFailure()).getMessage().contains("jwt.expire"));
        });
    }

    @Test
    void restartingWithTheSameKeyPreservesTokensButRotationRejectsThem() {
        String secret = randomKey(32);
        var old = new JwtUtil(sessions, secret, 60_000);
        String token = old.createToken(User.create().setId(42L).setPassword("hash"));
        assertTrue(new JwtUtil(sessions, secret, 60_000).validateToken(token));
        assertFalse(new JwtUtil(sessions, randomKey(32), 60_000).validateToken(token));
    }

    @Test
    void otherAlgorithmsAndUnsignedTokensCannotUseTheSameKey() {
        String secret = randomKey(64);
        var jwt = new JwtUtil(sessions, secret, 60_000);
        String otherAlgorithm = Jwts.builder().subject("42")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)), Jwts.SIG.HS512).compact();
        String unsigned = Jwts.builder().subject("42").compact();
        assertFalse(jwt.validateToken(otherAlgorithm));
        assertFalse(jwt.validateToken(unsigned));
    }

    private static Throwable rootCause(Throwable cause) {
        while (cause.getCause() != null) cause = cause.getCause();
        return cause;
    }
}
