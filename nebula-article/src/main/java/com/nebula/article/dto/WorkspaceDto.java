package com.nebula.article.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/** 编辑备份允许未写完的内容，不代表保存或发布文章。 */
public record WorkspaceDto(@NotNull @PositiveOrZero Long revision, @Valid Draft draft) {
    public record Draft(@NotNull @Size(max = 255) String title,
                        @NotNull @Size(max = 200000) String content,
                        @Positive Long categoryId,
                        @NotNull @Size(max = 100) List<@NotNull @Positive Long> tagIds,
                        @NotNull @PositiveOrZero Long sourceVersion) {}
}
