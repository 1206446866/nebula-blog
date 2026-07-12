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
    ARTICLE_TAG_BIND_FAILED(40409, "文章标签绑定失败");


    private final Integer code;

    private final String message;
}