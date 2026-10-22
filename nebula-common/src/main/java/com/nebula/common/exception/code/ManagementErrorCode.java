package com.nebula.common.exception.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ManagementErrorCode implements ErrorCode {
    INVALID_PAGINATION(40030, "页码须大于零，每页条数须为1至100", 400),
    CATEGORY_INVALID(40031, "请选择有效且未删除的分类", 400),
    TAG_INVALID(40032, "包含无效或已删除的标签", 400),
    CATEGORY_IN_USE(40902, "该分类仍被文章使用，请先调整文章分类", 409),
    TAG_IN_USE(40903, "该标签仍被文章使用，请先移除文章中的标签", 409);

    private final Integer code;
    private final String message;
    private final int httpStatus;
}
