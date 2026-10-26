package com.nebula.common.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 高成本入口的共享频率预算及每个应用实例的并发上限。 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "nebula.resources")
public class ResourceLimitProperties {
    @NotBlank private String keyPrefix = "nebula:resources:v1:";
    @Min(1) private int windowSeconds = 60;
    @Min(1) private int maxConcurrency = 16;
    @Min(1) private int decodeConcurrency = 2;
    @Min(1) private int searchTimeoutSeconds = 3;
    @Valid private Rate upload = new Rate(12, 40, 4, 1, 2);
    @Valid private Rate search = new Rate(30, 120, 8, 1, 3);
    @Valid private Rate read = new Rate(240, 600, 16, 4, 8);
    @Valid private Rate write = new Rate(60, 180, 8, 2, 4);

    @Data
    public static class Rate {
        @Min(1) private int account;
        @Min(1) private int ip;
        @Min(1) private int concurrency;
        @Min(1) private int accountConcurrency = 1;
        @Min(1) private int ipConcurrency = 2;
        public Rate() {}
        public Rate(int account, int ip, int concurrency) {
            this(account, ip, concurrency, concurrency, concurrency);
        }
        public Rate(int account, int ip, int concurrency, int accountConcurrency, int ipConcurrency) {
            this.account = account; this.ip = ip; this.concurrency = concurrency;
            this.accountConcurrency = accountConcurrency; this.ipConcurrency = ipConcurrency;
        }
    }
}
