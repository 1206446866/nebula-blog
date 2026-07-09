package com.nebula.comment.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

@Table("comment_like")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class CommentLike extends Model<CommentLike> {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long userId;

    private Long commentId;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    public static CommentLike create(){
        return new CommentLike();
    }
}
