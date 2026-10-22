package com.nebula.start.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Validate external credentials before serving requests; never retain or log their values. */
@Component
public final class DatabaseCredentialValidation {
    public DatabaseCredentialValidation(@Value("${spring.datasource.username}") String username,
                                        @Value("${spring.datasource.password}") String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Configure NEBULA_DB_USERNAME with a dedicated application account");
        }
        if ("root".equalsIgnoreCase(username.strip())) {
            throw new IllegalStateException("The application must use a dedicated non-root database account");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Configure a non-empty NEBULA_DB_PASSWORD");
        }
    }
}
