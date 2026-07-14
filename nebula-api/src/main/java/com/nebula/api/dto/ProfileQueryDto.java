package com.nebula.api.dto;

import com.nebula.common.constant.PageConstant;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ProfileQueryDto {

    @Min(1)
    private int publicArticlePage = PageConstant.DEFAULT_PAGE;

    @Min(1)
    @Max(100)
    private int publicArticleSize = PageConstant.DEFAULT_SIZE;

    @Min(1)
    private int draftArticlePage = PageConstant.DEFAULT_PAGE;

    @Min(1)
    @Max(100)
    private int draftArticleSize = PageConstant.DEFAULT_SIZE;

    @Min(1)
    private int commentPage = PageConstant.DEFAULT_PAGE;

    @Min(1)
    @Max(100)
    private int commentSize = PageConstant.DEFAULT_SIZE;

}
