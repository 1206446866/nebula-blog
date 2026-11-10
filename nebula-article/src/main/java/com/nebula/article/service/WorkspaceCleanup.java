package com.nebula.article.service;

import com.mybatisflex.core.row.Db;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 清理过期正文但保留版本号，旧标签页不能借清理过程覆盖新版备份。 */
@Component
public class WorkspaceCleanup {
    /** 每小时清空超过30天的备份正文，保留并发检查使用的版本号。 */
    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    public void clearExpired() {
        Db.updateBySql("UPDATE article_workspace SET payload=NULL WHERE payload IS NOT NULL AND updated_at < DATE_SUB(NOW(),INTERVAL 30 DAY)");
    }
}
