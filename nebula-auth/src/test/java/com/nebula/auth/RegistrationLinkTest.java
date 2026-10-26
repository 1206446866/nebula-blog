package com.nebula.auth;

import com.nebula.auth.security.RegistrationProperties;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 确认邮件使用配置的可信页面，并完整编码凭证，不向真实邮箱发信。 */
class RegistrationLinkTest {
    @Test void configuredAddressAndEncodedTokenAreUsed() {
        var properties = new RegistrationProperties();
        properties.setConfirmationUrl("https://blog.example.invalid/register");
        assertTrue(properties.isConfirmationUrlValid());
        assertEquals("https://blog.example.invalid/register?token=a%26b%2Bc", properties.confirmationLink("a&b+c"));
    }
    @Test void unsafeOrAmbiguousAddressesFailValidation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var properties = new RegistrationProperties();
            for (String address : new String[]{"javascript:alert(1)","//example.invalid/register","https://user:password@example.invalid/register","https://example.invalid/register?next=1","https://example.invalid/register#part",""}) {
                properties.setConfirmationUrl(address);
                assertFalse(factory.getValidator().validate(properties).isEmpty(), address);
            }
        }
    }
}
