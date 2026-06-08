package com.nebula.tag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data(staticConstructor = "create")
@Accessors(chain = true)
public class CreateTagDTO {

    @NotBlank(message = "标签名称不能为空")
    private String name;

    private String description;
}