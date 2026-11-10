package com.nebula.article.vo;

import com.nebula.article.dto.WorkspaceDto;

public final class ArticleRevisionVO {
    private ArticleRevisionVO() {}
    public record Summary(long id, long version, String title, String createdAt) {}
    public record Detail(long id, long version, WorkspaceDto.Draft draft) {}
}
