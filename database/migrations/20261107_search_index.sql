-- 搜索任务与业务写入同事务提交；索引只保存派生副本，不修改业务记录。
-- 通过scripts/ApplySearchMigration.java显式执行；不授予应用账号DDL或TRIGGER权限。
CREATE TABLE IF NOT EXISTS search_sync_task (
 kind VARCHAR(16) NOT NULL,target_id BIGINT NOT NULL,generation BIGINT NOT NULL DEFAULT 1,
 applied_generation BIGINT NOT NULL DEFAULT 0,applied_epoch BIGINT NOT NULL DEFAULT 0,
 dirty TINYINT NOT NULL DEFAULT 1,cascade_cursor BIGINT NOT NULL DEFAULT 0,cascade_done TINYINT NOT NULL DEFAULT 1,
 attempts INT NOT NULL DEFAULT 0,last_error VARCHAR(64) NULL,
 next_attempt_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 PRIMARY KEY(kind,target_id),INDEX idx_search_pending(dirty,next_attempt_at,updated_at)
) ENGINE=InnoDB CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS search_index_state (
 id INT PRIMARY KEY,epoch BIGINT NOT NULL DEFAULT 0,index_name VARCHAR(128) NULL,
 previous_index VARCHAR(128) NULL,retired_index VARCHAR(128) NULL,ready TINYINT NOT NULL DEFAULT 0,
 rebuild_requested TINYINT NOT NULL DEFAULT 1,last_error VARCHAR(64) NULL,
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB CHARSET=utf8mb4;
INSERT IGNORE INTO search_index_state(id) VALUES(1);

CREATE TRIGGER nebula_search_article_i AFTER INSERT ON `article` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'article',NEW.id,0 FROM DUAL
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=0,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

CREATE TRIGGER nebula_search_article_u AFTER UPDATE ON `article` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'article',NEW.id,0 FROM DUAL WHERE NOT (BINARY OLD.title<=>BINARY NEW.title) OR NOT (BINARY OLD.content<=>BINARY NEW.content) OR NOT (BINARY OLD.author<=>BINARY NEW.author) OR NOT (BINARY OLD.status<=>BINARY NEW.status) OR NOT (BINARY OLD.deleted<=>BINARY NEW.deleted) OR NOT (BINARY OLD.create_time<=>BINARY NEW.create_time)
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=0,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

CREATE TRIGGER nebula_search_article_d AFTER DELETE ON `article` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'article',OLD.id,0 FROM DUAL
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=0,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

CREATE TRIGGER nebula_search_comment_i AFTER INSERT ON `comment` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'comment',NEW.id,1 FROM DUAL
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=1,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

CREATE TRIGGER nebula_search_comment_u AFTER UPDATE ON `comment` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'comment',NEW.id,1 FROM DUAL WHERE NOT (BINARY OLD.content<=>BINARY NEW.content) OR NOT (BINARY OLD.article_id<=>BINARY NEW.article_id) OR NOT (BINARY OLD.status<=>BINARY NEW.status) OR NOT (BINARY OLD.create_time<=>BINARY NEW.create_time)
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=1,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

CREATE TRIGGER nebula_search_comment_d AFTER DELETE ON `comment` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'comment',OLD.id,1 FROM DUAL
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=1,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

CREATE TRIGGER nebula_search_user_i AFTER INSERT ON `user` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'user',NEW.id,1 FROM DUAL
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=1,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

CREATE TRIGGER nebula_search_user_u AFTER UPDATE ON `user` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'user',NEW.id,1 FROM DUAL WHERE NOT (BINARY OLD.username<=>BINARY NEW.username) OR NOT (BINARY OLD.nid<=>BINARY NEW.nid) OR NOT (BINARY OLD.status<=>BINARY NEW.status) OR NOT (BINARY OLD.deleted<=>BINARY NEW.deleted) OR NOT (BINARY OLD.create_time<=>BINARY NEW.create_time)
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=1,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

CREATE TRIGGER nebula_search_user_d AFTER DELETE ON `user` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'user',OLD.id,1 FROM DUAL
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=1,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

CREATE TRIGGER nebula_search_category_i AFTER INSERT ON `category` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'category',NEW.id,1 FROM DUAL
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=1,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

CREATE TRIGGER nebula_search_category_u AFTER UPDATE ON `category` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'category',NEW.id,1 FROM DUAL WHERE NOT (BINARY OLD.name<=>BINARY NEW.name) OR NOT (BINARY OLD.description<=>BINARY NEW.description) OR NOT (BINARY OLD.deleted<=>BINARY NEW.deleted) OR NOT (BINARY OLD.create_time<=>BINARY NEW.create_time)
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=1,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

CREATE TRIGGER nebula_search_category_d AFTER DELETE ON `category` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'category',OLD.id,1 FROM DUAL
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=1,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

CREATE TRIGGER nebula_search_tag_i AFTER INSERT ON `tag` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'tag',NEW.id,1 FROM DUAL
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=1,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

CREATE TRIGGER nebula_search_tag_u AFTER UPDATE ON `tag` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'tag',NEW.id,1 FROM DUAL WHERE NOT (BINARY OLD.name<=>BINARY NEW.name) OR NOT (BINARY OLD.description<=>BINARY NEW.description) OR NOT (BINARY OLD.deleted<=>BINARY NEW.deleted) OR NOT (BINARY OLD.create_time<=>BINARY NEW.create_time)
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=1,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

CREATE TRIGGER nebula_search_tag_d AFTER DELETE ON `tag` FOR EACH ROW
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'tag',OLD.id,1 FROM DUAL
ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,cascade_cursor=0,cascade_done=1,attempts=0,next_attempt_at=CURRENT_TIMESTAMP(6);

-- 初始补齐全部身份和墓碑；重复执行不覆盖已有版本或确认进度。
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'article',id,1 FROM `article` WHERE id>0 ON DUPLICATE KEY UPDATE kind=VALUES(kind);
-- 初始补齐全部身份和墓碑；重复执行不覆盖已有版本或确认进度。
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'comment',id,1 FROM `comment` WHERE id>0 ON DUPLICATE KEY UPDATE kind=VALUES(kind);
-- 初始补齐全部身份和墓碑；重复执行不覆盖已有版本或确认进度。
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'user',id,1 FROM `user` WHERE id>0 ON DUPLICATE KEY UPDATE kind=VALUES(kind);
-- 初始补齐全部身份和墓碑；重复执行不覆盖已有版本或确认进度。
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'category',id,1 FROM `category` WHERE id>0 ON DUPLICATE KEY UPDATE kind=VALUES(kind);
-- 初始补齐全部身份和墓碑；重复执行不覆盖已有版本或确认进度。
INSERT INTO search_sync_task(kind,target_id,cascade_done) SELECT 'tag',id,1 FROM `tag` WHERE id>0 ON DUPLICATE KEY UPDATE kind=VALUES(kind);
