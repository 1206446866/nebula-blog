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
    COMMENT_DELETE_FAILED(5005, "评论删除失败"),
    COMMENT_LIKE_FAILED(5006, "评论点赞状态更新失败，请重试"),
    COMMENT_REPLY_INVALID(5007, "回复目标无效或不属于当前文章"),
    COMMENT_INPUT_INVALID(5008, "评论须为1至2000个字符，文章与回复目标须有效"),
    COMMENT_REPLY_TOO_DEEP(5009, "这段讨论的回复已较多，请回复主评论继续交流");

    private final Integer code;
    private final String message;

    @Override
    public int getHttpStatus() {
        return switch (this) {
            case COMMENT_NOT_FOUND -> 404;
            case COMMENT_ACCESS_DENIED -> 403;
            case COMMENT_LIKE_FAILED -> 409;
            case COMMENT_REPLY_INVALID, COMMENT_INPUT_INVALID, COMMENT_REPLY_TOO_DEEP -> 400;
            default -> 500;
        };
    }
}
