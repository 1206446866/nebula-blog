package com.nebula.api;

import com.nebula.api.dto.ProfileQueryDto;
import com.nebula.auth.dto.ChangePasswordDTO;
import com.nebula.user.dto.UpdateNameDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileValidationTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeValidator() { FACTORY.close(); }

    @Test
    void nicknamesAreValidatedAsTextLengths() {
        UpdateNameDTO dto = new UpdateNameDTO();
        dto.setUsername("星云漫游者");
        assertTrue(VALIDATOR.validate(dto).isEmpty());
        dto.setUsername("ab");
        assertFalse(VALIDATOR.validate(dto).isEmpty());
        dto.setUsername("x".repeat(21));
        assertFalse(VALIDATOR.validate(dto).isEmpty());
    }

    @Test
    void passwordFieldsCannotBeMissingOrBlank() {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("old-password");
        dto.setNewPassword("   ");
        assertFalse(VALIDATOR.validate(dto).isEmpty());
        dto.setNewPassword("new-password");
        dto.setConfirmPassword("new-password");
        assertTrue(VALIDATOR.validate(dto).isEmpty());
    }

    @Test
    void paginationRejectsZeroPagesAndUnboundedSizes() {
        ProfileQueryDto dto = new ProfileQueryDto();
        assertTrue(VALIDATOR.validate(dto).isEmpty());
        dto.setCommentPage(0);
        dto.setPublicArticleSize(101);
        assertEquals(2, VALIDATOR.validate(dto).size());
    }
}
