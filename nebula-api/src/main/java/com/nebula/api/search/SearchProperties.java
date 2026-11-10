package com.nebula.api.search;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 全站检索连接、索引范围和同步预算；凭据由本地配置树或环境变量提供。 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "nebula.search")
public class SearchProperties {
    @Pattern(regexp = "elasticsearch|mysql") private String backend = "elasticsearch";
    @NotBlank private String endpoint = "http://127.0.0.1:9200";
    @Size(max = 80) @Pattern(regexp = "nebula-search(?:-[a-z0-9]+)*") private String alias = "nebula-search";
    @NotBlank private String username = "nebula_search";
    @lombok.ToString.Exclude private String password = "";
    @Min(100) @Max(3000) private int requestTimeoutMs = 1800;
    @Min(1) @Max(500) private int batchSize = 200;
    @Min(1000) @Max(10000) private int maxWindow = 10000;
    private boolean synchronizationEnabled = true;
}
