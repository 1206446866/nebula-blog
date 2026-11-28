package com.nebula.user.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 游客可见的用户资料，公开资源地址不携带上传目录中的登录账号。 */
public record PublicUserVO(Long id, String username, String avatar,
                           @JsonFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime createTime) {
    /** 保留公开的外链头像，本地头像通过用户编号读取。 */
    public static String avatarUrl(Long id, String avatar) {
        if (avatar == null || avatar.isBlank()) return null;
        if (avatar.startsWith("upload/avatar/") || avatar.startsWith("/upload/avatar/")
                || avatar.matches("[A-Za-z0-9_-]+\\.(?i:jpg|jpeg|png|gif|webp)")) {
            return "upload/public-avatar/" + id;
        }
        return avatar;
    }
}
