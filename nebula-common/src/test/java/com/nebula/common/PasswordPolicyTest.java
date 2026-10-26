package com.nebula.common;

import com.nebula.common.util.PasswordPolicy;
import com.nebula.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    @Test void checksCharacterAndByteBoundariesWithoutBlacklistOrTrimming() {
        for (String value : new String[]{"123456", "password", "a".repeat(64), "中".repeat(24), "😀".repeat(6), " 1234 "})
            assertTrue(PasswordPolicy.isValidNewPassword(value), value);
        for (String value : new String[]{"12345", " ".repeat(6), "a".repeat(65), "中".repeat(25), "😀".repeat(19)})
            assertThrows(BusinessException.class, () -> PasswordPolicy.validateNewPassword(value));
        assertFalse(PasswordPolicy.isValidNewPassword(null));
    }
}
