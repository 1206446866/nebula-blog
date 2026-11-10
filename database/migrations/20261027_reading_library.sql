-- 阅读记录和收藏均按账号隔离；文章撤回或删除后不再对读者返回内容。
CREATE TABLE IF NOT EXISTS article_reading (
  user_id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  favorite TINYINT NOT NULL DEFAULT 0,
  later TINYINT NOT NULL DEFAULT 0,
  progress INT NOT NULL DEFAULT 0,
  anchor VARCHAR(160) NOT NULL DEFAULT '',
  read_at DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(user_id, article_id),
  KEY idx_reading_recent(user_id, read_at),
  KEY idx_reading_saved(user_id, favorite, later, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
