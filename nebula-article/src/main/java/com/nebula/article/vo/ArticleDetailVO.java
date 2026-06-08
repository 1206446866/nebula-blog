package com.nebula.article.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ArticleDetailVO {

    private Long id;

    private String title;

    private String content;

    private Long categoryId;

    private List<Long> tagIds;
}