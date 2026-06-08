package com.nebula.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 修改文章DTO
 *
 * @author Nebula
 */
@Data(staticConstructor = "create")
@Accessors(chain = true)
public class UpdateArticleDto {

    /**
     * 文章ID
     */
    @NotNull(message = "文章ID不能为空")
    private Long id;

    /**
     * 文章标题
     */
    @NotBlank(message = "文章标题不能为空")
    private String title;

    /**
     * 文章内容
     */
    @NotBlank(message = "文章内容不能为空")
    private String content;

    private Long categoryId;

    private List<Long> tagIds;
}