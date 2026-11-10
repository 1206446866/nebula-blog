package com.nebula.article.vo;

import java.util.List;

/** 私人阅读状态和分页结果，不包含文章正文。 */
public final class ReadingVO {
    private ReadingVO() {}
    public record State(boolean favorite, boolean later, int progress, String anchor, String readAt) {}
    public record Item(Long articleId, String title, String author, State state) {}
    public record Library(List<Item> records, long total, int current, int size) {}
}
