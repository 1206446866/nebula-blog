package com.nebula.common.exception.code;

public interface ErrorCode {

    /** * 错误码 */
    Integer getCode();

    /** * 错误信息 */
    String getMessage();

    /** HTTP status is explicit; legacy business codes are not HTTP status codes. */
    default int getHttpStatus() {
        return 400;
    }
}
