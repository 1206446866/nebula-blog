package com.nebula.comment.dto;

import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 发布评论的实体类
 */
@Data
@Accessors(chain=true)
@AllArgsConstructor
public class ReleaseCommentDto {
    @NotNull
    @Min(1)
    private Long articleId;

    @NotBlank
    private String content;
}
