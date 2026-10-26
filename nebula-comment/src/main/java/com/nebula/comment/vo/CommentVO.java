package com.nebula.comment.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data(staticConstructor = "create")
@Accessors(chain = true)
public class CommentVO {

    private Long id;
    private Long userId;
    private String username;
    private Long articleId;
    private Long parentId;
    private Long rootId;
    private Long replyToUserId;
    private String replyToUsername;
    private String avatar;
    private Long likeCount;
    private Long replyCount;
    private String content;
    private LocalDateTime createTime;
    private Boolean liked;
}
