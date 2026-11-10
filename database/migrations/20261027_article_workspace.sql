-- 私人编辑备份独立于文章正文，版本号用于拒绝过期页面的写入。
ALTER TABLE article ADD COLUMN edit_version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS article_workspace (
  user_id BIGINT NOT NULL,
  document_key VARCHAR(40) NOT NULL,
  revision BIGINT NOT NULL DEFAULT 0,
  payload MEDIUMTEXT NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (user_id, document_key),
  KEY idx_workspace_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
