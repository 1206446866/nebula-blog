package com.nebula.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ProfileQueryDto {

    @Min(1)
    private int publicArticlePage = 1;

    @Min(1)
    @Max(100)
    private int publicArticleSize = 10;

    @Min(1)
    private int draftArticlePage = 1;

    @Min(1)
    @Max(100)
    private int draftArticleSize = 10;

    @Min(1)
    private int commentPage = 1;

    @Min(1)
    @Max(100)
    private int commentSize = 10;

}
