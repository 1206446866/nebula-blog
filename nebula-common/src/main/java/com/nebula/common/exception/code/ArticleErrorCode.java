package com.nebula.common.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文章错误码
 */
@Getter
@AllArgsConstructor
public enum ArticleErrorCode implements ErrorCode {

    /**
     * 文章不存在
     */
    ARTICLE_NOT_FOUND(40401, "文章不存在"),

    /**
     * 文章未发布
     */
    ARTICLE_NOT_PUBLISHED(40402, "文章未发布"),

    /**
     * 文章创建失败
     */
    ARTICLE_CREATE_FAILED(40403, "文章创建失败"),

    /**
     * 文章更新失败
     */
    ARTICLE_UPDATE_FAILED(40404, "文章更新失败"),

    /**
     * 文章删除失败
     */
    ARTICLE_DELETE_FAILED(40405, "文章删除失败"),

    /**
     * 文章状态修改失败
     */
    ARTICLE_STATUS_UPDATE_FAILED(40406, "文章状态修改失败"),

    /**
     * 文章无权操作
     */
    ARTICLE_ACCESS_DENIED(40407, "无权操作该文章"),

    /**
     * 文章分类绑定失败
     */
    ARTICLE_CATEGORY_BIND_FAILED(40408, "文章分类绑定失败"),

    /**
     * 文章标签绑定失败
     */
    ARTICLE_TAG_BIND_FAILED(40409, "文章标签绑定失败"),

    ARTICLE_LIKE_FAILED(40901, "点赞状态更新失败，请重试"),
    ARTICLE_STATUS_INVALID(40024, "文章状态非法"),
    SCHEDULE_TIME_INVALID(40025, "请选择未来的发布时间，并提供时区"),
    SCHEDULE_DRAFT_REQUIRED(40026, "仅草稿可以安排定时发布，请先撤回已发布文章"),
    SCHEDULE_CONTENT_INVALID(40027, "请先完善文章标题、正文和有效分类");


    private final Integer code;

    private final String message;

    @Override
    public int getHttpStatus() {
        return switch (this) {
            case ARTICLE_NOT_FOUND, ARTICLE_NOT_PUBLISHED -> 404;
            case ARTICLE_ACCESS_DENIED -> 403;
            case ARTICLE_LIKE_FAILED -> 409;
            case ARTICLE_STATUS_INVALID, SCHEDULE_TIME_INVALID, SCHEDULE_DRAFT_REQUIRED, SCHEDULE_CONTENT_INVALID -> 400;
            default -> 500;
        };
    }
}
