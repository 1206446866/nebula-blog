package com.nebula.article.entity;

import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Table("article_category")
@Accessors(chain = true) // 关键，开启链式
@Data(staticConstructor = "create")
@EqualsAndHashCode(callSuper = true)
public class ArticleCategory extends Model <ArticleCategory>{

    private Long articleId;

    private Long categoryId;
}
