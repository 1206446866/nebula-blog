package com.nebula.media.entity;

import com.mybatisflex.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@Table("media_asset")
public class MediaAsset {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long userId;
    private String business;
    private String path;
    private String originalName;
    private String contentType;
    private Long size;
    private Integer width;
    private Integer height;
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;
    @Column(isLogicDelete = true)
    private Integer deleted;
}
