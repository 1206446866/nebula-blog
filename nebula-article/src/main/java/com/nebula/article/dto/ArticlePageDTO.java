package com.nebula.article.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data(staticConstructor = "create")
@Accessors(chain = true)
public class ArticlePageDTO {

    private Long userId;

    private String title;

    private String author;

    @Size(max = 100)
    private List<@NotNull @Positive Long> tagIds;

    @Size(max = 100)
    private List<@NotNull @Positive Long> categoryIds;

    @Min(1)
    private int current = 1;

    @Min(1)
    @Max(100)
    private int size = 10;
}
