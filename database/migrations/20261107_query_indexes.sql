-- 使用MyBatis-Flex迁移入口执行；先检查information_schema.statistics，已存在的同名索引不重复创建。
CREATE INDEX idx_article_public_order ON article(deleted,status,create_time,id);
CREATE INDEX idx_article_owner_order ON article(user_id,status,deleted,create_time,id);
CREATE INDEX idx_comment_thread ON comment(article_id,status,parent_id,create_time,id);
CREATE INDEX idx_comment_owner_order ON comment(user_id,create_time,id,article_id);
CREATE INDEX idx_comment_owner_stats ON comment(user_id,status,article_id,like_count);
