package com.nebula.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 发布评论的实体类
 */
@Data
@Accessors(chain=true)
@AllArgsConstructor
public class ReleaseCommentDto {
    private Long articleId;

    private String content;
}
