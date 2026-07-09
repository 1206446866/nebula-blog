package com.nebula.comment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentLikeDto {

    @NotNull(message = "该评论已经消失不见了哦~")
    private Long commentId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
