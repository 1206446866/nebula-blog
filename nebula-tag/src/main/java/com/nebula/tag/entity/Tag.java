package com.nebula.tag.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


@Table("tag")
@Accessors(chain = true) // 关键，开启链式
@Data(staticConstructor = "create")
@EqualsAndHashCode(callSuper = true)
public class Tag extends Model<Tag> {

    private Long id;

    private String name;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer status; // 1: 正常, 0: 删除
}