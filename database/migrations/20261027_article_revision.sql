-- 仅手动保存产生版本，自动编辑备份不会制造大量历史记录。
CREATE TABLE IF NOT EXISTS article_revision (
  id BIGINT NOT NULL AUTO_INCREMENT,
  article_id BIGINT NOT NULL,
  edit_version BIGINT NOT NULL,
  editor_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  payload MEDIUMTEXT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id),
  UNIQUE KEY uk_revision_version(article_id, edit_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
