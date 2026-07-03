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


@Table("article")
@Accessors(chain = true) // 关键，开启链式
@Data
@EqualsAndHashCode(callSuper = true)
public class Article extends Model<Article> {

    @Id(keyType = KeyType.Auto,comment = "文章ID" )
    private Long id;
    /**
     * 标题
     */
    private String title;
    /**
     * 内容
     */
    private String content;
    /**
     * 作者ID
     */
    private Long userId;
    /**
     * 作者
     */
    private String author;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;

    @Column(comment = "0: 草稿, 1: 已发布")
    private Integer status;

    @Column(isLogicDelete = true, comment = "逻辑删除 0未删除 1已删除")
    private Integer deleted;

    @Column(comment = "浏览量")
    private Long viewCount;

    /**
     * 点赞数量
     */
    private Integer likeCount;
    public static Article create() {
        return new Article();
    }
}