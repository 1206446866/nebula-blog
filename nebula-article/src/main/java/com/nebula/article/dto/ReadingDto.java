package com.nebula.article.dto;

import jakarta.validation.constraints.*;

public final class ReadingDto {
    private ReadingDto() {}
    /** 显式设置目标状态，重试请求不会产生二次切换。 */
    public record Flags(@NotNull Boolean favorite, @NotNull Boolean later) {}
    /** 阅读比例使用万分比，定位失败时仍能按比例续读。 */
    public record Progress(@Min(0) @Max(10000) int progress, @NotNull @Size(max = 160) String anchor) {}
}
