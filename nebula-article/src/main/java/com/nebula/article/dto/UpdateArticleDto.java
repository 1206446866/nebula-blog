package com.nebula.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    @Positive
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

    @NotNull(message = "必须选择一个分类")
    @Positive
    private Long categoryId;

    @Size(max = 100)
    private List<@NotNull @Positive Long> tagIds;
}
