package com.nebula.comment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CommentLikeDto {

    @NotNull(message = "该评论已经消失不见了哦~")
    @Positive
    private Long commentId;
}
