package com.nebula.article.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class SchedulePublicationDTO {
    /** 必须包含时区偏移，前端默认使用 +08:00。 */
    @NotNull(message = "请选择发布时间")
    private OffsetDateTime publishAt;
}
