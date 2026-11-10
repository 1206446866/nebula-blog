package com.nebula.article.vo;

import java.util.List;

public final class ArticleSeriesVO {
    private ArticleSeriesVO() {}
    public record Summary(long id, long userId, String title, String description, boolean publicVisible, int articleCount) {}
    public record Item(long id, String title, int status) {}
    public record Detail(Summary series, List<Item> articles) {}
    public record Listing(List<Summary> records, long total, int current, int size) {}
}
