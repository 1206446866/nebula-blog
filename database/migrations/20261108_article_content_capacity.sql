-- 正文容量覆盖编辑备份允许的20万字符；只扩大容量，不修改正文。
-- ApplySearchMigration入口执行时显式保留现有列字符集、排序规则及可空约束。
ALTER TABLE article MODIFY COLUMN content MEDIUMTEXT NOT NULL COMMENT '文章内容';
