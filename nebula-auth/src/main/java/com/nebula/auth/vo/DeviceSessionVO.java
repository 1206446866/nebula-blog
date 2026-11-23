package com.nebula.auth.vo;

import java.time.Instant;

/** 用户本人可见的活动会话信息。 */
public record DeviceSessionVO(String id, String browser, String os, Instant signedInAt,
                              Instant expiresAt, boolean current) {
}
