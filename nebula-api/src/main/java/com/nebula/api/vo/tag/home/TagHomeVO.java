package com.nebula.api.vo.tag.home;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TagHomeVO {

    private Long id;

    private String name;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long articleCount;

    public static TagHomeVO create(){
        return new TagHomeVO();
    }
}
