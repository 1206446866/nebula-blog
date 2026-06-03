package com.nebula.article.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 修改文章状态请求参数
 */
@Data(staticConstructor = "create")
@Accessors(chain = true)
public class ChangeArticleStatusDto {

    /**
     * 文章ID
     */
    @NotNull(message = "文章ID不能为空")
    private Long id;

    /**
     * 状态
     * 0-草稿
     * 1-已发布
     */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态非法")
    @Max(value = 1, message = "状态非法")
    private Integer status;
}