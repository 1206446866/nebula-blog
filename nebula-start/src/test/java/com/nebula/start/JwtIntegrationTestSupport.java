package com.nebula.start;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Each test JVM has a fresh key, independent of the developer/deployment secret. */
abstract class JwtIntegrationTestSupport {
    private static final String TEST_KEY = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> TEST_KEY);
    }
}
