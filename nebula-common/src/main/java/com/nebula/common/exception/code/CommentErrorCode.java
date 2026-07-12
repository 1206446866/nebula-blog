package com.nebula.common.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommentErrorCode implements ErrorCode {

    /**
     * 评论不存在
     */
    COMMENT_NOT_FOUND(5001, "评论不存在"),

    /**
     * 评论无权操作
     */
    COMMENT_ACCESS_DENIED(5002, "无权操作该评论"),

    /**
     * 评论创建失败
     */
    COMMENT_CREATE_FAILED(5003, "评论创建失败"),

    /**
     * 评论更新失败
     */
    COMMENT_UPDATE_FAILED(5004, "评论更新失败"),

    /**
     * 评论删除失败
     */
    COMMENT_DELETE_FAILED(5005, "评论删除失败");

    private final Integer code;
    private final String message;
}