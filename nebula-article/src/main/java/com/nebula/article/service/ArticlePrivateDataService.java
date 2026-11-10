package com.nebula.article.service;

import com.mybatisflex.core.row.Db;
import com.nebula.common.security.AccountCancelledEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 在账号注销事务中清理私人阅读和编辑数据，不删除正式文章及公开内容。 */
@Service
public class ArticlePrivateDataService {
    /**
     * 清理新注销账号的编辑备份、书架及私人系列，任一失败均回滚账号注销。
     * @param event 已在当前事务中完成逻辑注销的账号
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void clearCancelledAccount(AccountCancelledEvent event) {
        long userId = event.userId();
        Db.deleteBySql("DELETE FROM article_workspace WHERE user_id=?", userId);
        Db.deleteBySql("DELETE FROM article_reading WHERE user_id=?", userId);
        Db.deleteBySql("DELETE i FROM article_series_item i JOIN article_series s ON s.id=i.series_id WHERE s.user_id=? AND s.public_visible=0", userId);
        Db.deleteBySql("DELETE FROM article_series WHERE user_id=? AND public_visible=0", userId);
    }

    /**
     * 写入私人数据前锁定正常账号，与注销使用同一账号行锁，避免迟到请求重新写入。
     * @param userId 当前写入账号的内部ID
     * @throws AccessDeniedException 账号已注销、停用或不存在
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockActiveAccount(long userId) {
        if (Db.selectOneBySql("SELECT id FROM user WHERE id=? AND deleted=0 AND status=0 FOR UPDATE", userId) == null)
            throw new AccessDeniedException("账号已失效，不能保存私人数据");
    }
}
