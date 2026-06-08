package com.nebula.tag.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


@Table("tag")
@Accessors(chain = true) // 关键，开启链式
@Data
@EqualsAndHashCode(callSuper = true)
public class Tag extends Model<Tag> {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private String name;

    private String description;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;

    @Column(isLogicDelete = true, comment = "逻辑删除 0未删除 1已删除")
    private Integer deleted;

    public static Tag create() {
        return new Tag();
    }
}