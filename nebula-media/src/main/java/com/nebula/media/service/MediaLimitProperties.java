package com.nebula.media.service;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 头像和文章图片共用账号配额；每日额度包含当天已经删除的上传。 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "nebula.media.limits")
public class MediaLimitProperties {
    @Min(1) private long accountBytes = 500L * 1024 * 1024;
    @Min(1) private int accountFiles = 2000;
    @Min(1) private long dailyBytes = 200L * 1024 * 1024;
    @Min(1) private int dailyFiles = 200;
    @Min(0) private long minFreeDiskBytes = 512L * 1024 * 1024;
    @Min(1) private long thumbnailCacheBytes = 16L * 1024 * 1024;
    @Min(1) private int thumbnailCacheEntries = 128;
}
