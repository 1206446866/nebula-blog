-- 已有数据库升级时执行，新建库的 user 表已包含该列。
ALTER TABLE `user` ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0 COMMENT '账号会话版本';
