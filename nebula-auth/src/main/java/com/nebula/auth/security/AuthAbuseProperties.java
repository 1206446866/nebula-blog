package com.nebula.auth.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "nebula.security.abuse")
public class AuthAbuseProperties {
    @NotBlank private String keyPrefix = "auth:abuse:";
    @Min(1) private int credentialAttempts = 5;
    @Min(1) private int credentialWindowSeconds = 900;
    @Min(1) private int loginIpRequests = 60;
    @Min(1) private int loginIpWindowSeconds = 60;
    @Min(1) private int emailIpRequests = 20;
    @Min(1) private int emailIpWindowSeconds = 3600;
    @Min(1) private int verifyIpRequests = 30;
    @Min(1) private int verifyIpWindowSeconds = 600;
    @Min(1) private int emailMailboxRequests = 5;
    @Min(1) private int emailMailboxWindowSeconds = 3600;
    @Min(1) private int emailCooldownSeconds = 60;
    @Min(1) private int captchaMaxFailures = 5;
}
