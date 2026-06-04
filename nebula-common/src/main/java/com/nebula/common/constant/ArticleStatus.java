package com.nebula.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文章状态
 *
 * <p>
 * DRAFT    草稿
 * PUBLISHED 已发布
 * OFFLINE 已下架
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum ArticleStatus {

    /**
     * 草稿
     */
    DRAFT(0, "草稿"),

    /**
     * 已发布
     */
    PUBLISHED(1, "已发布"),

    /**
     * 已下架
     */
    OFFLINE(2, "已下架");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 状态枚举
     */
    public static ArticleStatus fromCode(Integer code) {
        for (ArticleStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知文章状态：" + code);
    }

    public boolean isPublished() {
        return this == PUBLISHED;
    }

    public boolean isDraft() {
        return this == DRAFT;
    }

    public boolean isOffline() {
        return this == OFFLINE;
    }
}