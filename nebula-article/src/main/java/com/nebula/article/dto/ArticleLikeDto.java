package com.nebula.article.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleLikeDto {
    @NotNull(message = "文章ID不能为空")
    private Long articleId;
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
