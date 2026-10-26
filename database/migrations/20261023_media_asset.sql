-- Execute through MyBatis-Flex Db with a migration account, not the runtime account.
CREATE TABLE IF NOT EXISTS media_asset (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  business VARCHAR(20) NOT NULL COMMENT 'avatar/article',
  path VARCHAR(255) NOT NULL COMMENT 'upload/business/nid/yyyyMMdd/uuid.ext',
  original_name VARCHAR(200) NOT NULL,
  content_type VARCHAR(40) NOT NULL,
  size BIGINT NOT NULL,
  width INT NOT NULL,
  height INT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_media_path (path),
  KEY idx_media_owner (user_id, deleted, business, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
