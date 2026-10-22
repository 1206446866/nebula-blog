package com.nebula.article.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties("userId") // Older clients may send it; it never determines the actor.
public class ArticleLikeDto {
    @NotNull(message = "文章ID不能为空")
    @Positive(message = "文章ID非法")
    private Long articleId;
}
