package com.nebula.comment.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data(staticConstructor = "create")
@Accessors(chain = true)
public class CommentVO {

    private Long id;
    private String username;
    private Long articleId;
    private String content;
    private LocalDateTime createTime;
}
