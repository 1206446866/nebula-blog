package com.nebula.category.entity;

import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Table("article")
@Accessors(chain = true) // 关键，开启链式
@Data(staticConstructor = "create")
@EqualsAndHashCode(callSuper = true)
public class Category extends Model<Category> {

    private Long id;

    private String name;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer status;
}
