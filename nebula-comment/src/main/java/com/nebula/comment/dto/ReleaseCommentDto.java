package com.nebula.comment.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 发布评论的实体类
 */
@Data
@Accessors(chain=true)
@AllArgsConstructor
@NoArgsConstructor
public class ReleaseCommentDto {
    @NotNull
    @Min(1)
    private Long articleId;

    @NotBlank
    @Size(max = 2000)
    private String content;

    @Min(0)
    private Long parentId;
}
