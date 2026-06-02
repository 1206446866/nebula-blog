package com.nebula.article.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


@Table("article")
@Accessors(chain = true) // 关键，开启链式
@Data(staticConstructor = "create")
@EqualsAndHashCode(callSuper = true)
public class Article extends Model<Article> {

    @Id(keyType = KeyType.Auto)
    private Long id;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String author;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Column(isLogicDelete = true,comment = "1: 正常, 0: 删除")
    private Integer status;
}