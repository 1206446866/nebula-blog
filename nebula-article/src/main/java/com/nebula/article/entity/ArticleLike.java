package com.nebula.article.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Table("article_like")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class ArticleLike extends Model<ArticleLike> {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long userId;

    private Long articleId;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    public static ArticleLike create(){
        return new ArticleLike();
    }
}
