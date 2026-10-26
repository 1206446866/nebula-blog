package com.nebula.media;

import com.nebula.common.exception.code.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MediaError implements ErrorCode {
    INVALID_IMAGE(1801, "请选择有效的 JPG、PNG、GIF 或 WebP 图片（不超过10MB、2500万像素）", 400),
    NOT_FOUND(1802, "图片不存在或不可访问", 404),
    IN_USE(1803, "图片仍被文章或头像使用，请先移除引用再删除", 409),
    INVALID_PATH(1804, "资源路径非法", 400),
    ACCOUNT_QUOTA(1805, "媒体空间或图片数量已达上限，请先清理闲置图片", 409),
    DAILY_QUOTA(1806, "今日图片上传量已达上限，请明天再试", 409),
    STORAGE_LOW(1807, "存储空间不足，暂时无法上传图片", 507);
    private final Integer code;
    private final String message;
    private final int httpStatus;
}
