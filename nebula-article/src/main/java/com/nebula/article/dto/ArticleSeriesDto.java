package com.nebula.article.dto;

import jakarta.validation.constraints.*;
import java.util.List;

/** 顺序即阅读顺序，只能编排系列作者自己的文章。 */
public record ArticleSeriesDto(@NotBlank @Size(max = 120) String title,
                               @NotNull @Size(max = 2000) String description,
                               @NotNull Boolean publicVisible,
                               @NotNull @Size(max = 100) List<@NotNull @Positive Long> articleIds) {}
