package com.nebula.auth.util;

import java.util.Locale;

/** Match the project's case-insensitive, unique email identity. */
public final class RegistrationEmail {
    private RegistrationEmail() {}

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
