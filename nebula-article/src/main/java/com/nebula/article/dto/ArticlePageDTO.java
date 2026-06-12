package com.nebula.article.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data(staticConstructor = "create")
@Accessors(chain = true)
public class ArticlePageDTO {

    private Long userId;

    private String title;

    private String author;

    private Long tagId;

    private Long categoryId;

    @Min(1)
    private int currentPage = 1;

    @Min(1)
    @Max(100)
    private int size = 10;
}