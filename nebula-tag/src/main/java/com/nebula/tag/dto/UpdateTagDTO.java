package com.nebula.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data(staticConstructor = "create")
@Accessors(chain = true)
public class UpdateTagDTO {

    @NotNull(message = "标签ID不能为空")
    private Long id;

    @NotBlank(message = "标签名称不能为空")
    private String name;

    private String description;
}