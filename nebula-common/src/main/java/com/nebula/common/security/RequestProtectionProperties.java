package com.nebula.common.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 认证前的本实例保护与JSON解析上限，不依赖数据库或Redis可用。 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "nebula.security.request")
public class RequestProtectionProperties {
    @Min(1) @Max(3600) private int windowSeconds = 60;
    @Min(1) private int ipRequests = 1200;
    @Min(1) private int maxConcurrency = 32;
    @Min(1) private int ipConcurrency = 12;
    @Min(1) @Max(65536) private int maxSources = 4096;
    @Min(256) @Max(33554432) private int jsonBytes = 2 * 1024 * 1024;
    @Min(256) @Max(1048576) private int loginBytes = 8 * 1024;
    // 正文总读取时间另受容器单次阻塞读取时间约束，不能依靠持续滴流永久占用名额。
    @Min(100) @Max(120000) private int jsonReadTimeoutMs = 10000;
    @Min(1) @Max(30) private int readTimeoutSeconds = 5;
    @Min(1) @Max(30) private int authenticationTimeoutSeconds = 3;
}
