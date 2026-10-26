package com.nebula.user.entity;

import com.mybatisflex.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

/** 单向关注关系，取消关注时物理删除，联合主键防止重复关注。 */
@Data
@Accessors(chain = true)
@Table("user_follow")
public class UserFollow {
    @Id private Long followerId;
    @Id private Long followingId;
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;
}
