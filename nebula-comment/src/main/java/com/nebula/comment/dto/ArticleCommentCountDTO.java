package com.nebula.comment.dto;

import lombok.Data;

@Data
public class ArticleCommentCountDTO {
    private Long articleId;
    private Long commentCount;
}
