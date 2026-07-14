package com.nebula.api.vo.profile;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileCommentVO {

    private Long id;
    private Long articleId;
    private String articleTitle;
    private String content;
    private LocalDateTime createTime;
    private Long likeCount;
}
