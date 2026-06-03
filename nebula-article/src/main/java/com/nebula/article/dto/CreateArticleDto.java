package com.nebula.article.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 新增文章DTO
 *
 * @author Nebula
 */
@Data(staticConstructor = "create")
@Accessors(chain = true)
@AllArgsConstructor
public class CreateArticleDto {

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
}