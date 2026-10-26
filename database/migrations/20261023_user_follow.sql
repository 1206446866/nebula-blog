-- 使用迁移账号通过 MyBatis-Flex Db 执行；不向应用账号授予建表权限。
CREATE TABLE IF NOT EXISTS user_follow (
    follower_id BIGINT NOT NULL COMMENT '关注者ID',
    following_id BIGINT NOT NULL COMMENT '被关注者ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, following_id),
    KEY idx_following_time (following_id, create_time, follower_id),
    KEY idx_follower_time (follower_id, create_time, following_id),
    CONSTRAINT chk_follow_not_self CHECK (follower_id <> following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
