package com.nebula.common.util;

import com.nebula.common.exception.BusinessException;
import com.nebula.common.exception.code.ManagementErrorCode;

public final class PageLimits {
    private PageLimits() {}

    public static void requireValid(long current, long size) {
        if (current < 1 || size < 1 || size > 100) {
            throw new BusinessException(ManagementErrorCode.INVALID_PAGINATION);
        }
    }
}
