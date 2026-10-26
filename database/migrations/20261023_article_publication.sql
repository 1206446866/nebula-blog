-- 通过 MyBatis-Flex Db 使用迁移账号执行。已有环境仅执行一次；先核对字段是否存在。
ALTER TABLE article
    ADD COLUMN scheduled_publish_at DATETIME NULL COMMENT '上海时区计划发布时间',
    ADD COLUMN scheduled_by BIGINT NULL COMMENT '安排发布的管理员ID',
    ADD COLUMN schedule_error VARCHAR(200) NULL COMMENT '暂停原因，重新安排时清除',
    ADD KEY idx_article_schedule (status, deleted, scheduled_publish_at);
