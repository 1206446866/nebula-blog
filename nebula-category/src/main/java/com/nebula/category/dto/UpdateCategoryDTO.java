package com.nebula.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data(staticConstructor = "create")
@Accessors(chain = true)
public class UpdateCategoryDTO {

    /**
     * 分类ID
     */
    @NotNull(message = "错误的分类ID")
    private Long id;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /**
     * 分类描述
     */
    private String description;
}