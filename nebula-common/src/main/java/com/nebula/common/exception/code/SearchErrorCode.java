package com.nebula.common.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SearchErrorCode implements ErrorCode {
    INVALID_SEARCH(1901, "搜索词须包含可检索字符，最多50字符且不能含NUL，请使用有效的类型和页码"),
    SEARCH_WINDOW(1902, "搜索结果较多，请缩小关键词后继续查询");
    private final Integer code;
    private final String message;
}
