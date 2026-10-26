package com.nebula.common.security;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "nebula.security.sensitive-password")
public class SensitivePasswordProperties {
    @NotBlank private String keyPrefix = "nebula:sensitive-password:v1:";
    @Min(1) private int attempts = 5;
    @Min(1) private int windowSeconds = 900;
    @Min(1) private int ipRequests = 30;
    @Min(1) private int ipWindowSeconds = 60;
}
