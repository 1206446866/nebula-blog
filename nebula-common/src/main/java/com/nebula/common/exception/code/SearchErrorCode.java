package com.nebula.common.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SearchErrorCode implements ErrorCode {
    INVALID_SEARCH(1901, "搜索关键词不能超过50个字符，请使用有效的类型和页码");
    private final Integer code;
    private final String message;
}
