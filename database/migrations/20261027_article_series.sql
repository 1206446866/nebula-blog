-- 系列默认私有，作者明确公开后才会出现在读者入口中。
CREATE TABLE IF NOT EXISTS article_series (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  description VARCHAR(2000) NOT NULL DEFAULT '',
  public_visible TINYINT NOT NULL DEFAULT 0,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id),
  KEY idx_series_owner(user_id, updated_at),
  KEY idx_series_public(public_visible, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS article_series_item (
  series_id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  position INT NOT NULL,
  PRIMARY KEY(series_id, article_id),
  UNIQUE KEY uk_series_position(series_id, position),
  KEY idx_series_article(article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
