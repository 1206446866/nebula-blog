package com.nebula.article.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.nebula.article.entity.Article;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.article.service.ArticlePublicationService;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.redis.RedisService;
import com.nebula.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import static com.nebula.article.entity.table.ArticleTableDef.ARTICLE;
import static com.nebula.common.exception.code.ArticleErrorCode.*;
import static com.nebula.common.constant.redisKey.ArticleRedisKey.ARTICLE_DETAIL;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticlePublicationServiceImpl implements ArticlePublicationService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final ArticleMapper articles;
    private final RedisService cache;
    private final PlatformTransactionManager transactions;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LocalDateTime schedule(Long articleId, OffsetDateTime publishAt) {
        requirePublisher();
        if (publishAt == null || publishAt.getYear() > 9999 || !publishAt.toInstant().isAfter(Instant.now())) throw new BusinessException(SCHEDULE_TIME_INVALID);
        Article article = lock(articleId);
        if (!Integer.valueOf(0).equals(article.getStatus())) throw new BusinessException(SCHEDULE_DRAFT_REQUIRED);
        if (!validContent(article)) throw new BusinessException(SCHEDULE_CONTENT_INVALID);
        LocalDateTime time = publishAt.atZoneSameInstant(ZONE).toLocalDateTime().withNano(0);
        if (!time.isAfter(LocalDateTime.now(ZONE))) throw new BusinessException(SCHEDULE_TIME_INVALID);
        Db.updateBySql("UPDATE article SET scheduled_publish_at=?, scheduled_by=?, schedule_error=NULL, update_time=NOW() WHERE id=? AND deleted=0",
                time, SecurityUtils.getUserId(), articleId);
        evictAfterCommit(articleId);
        return time;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long articleId) {
        requirePublisher();
        Article article = lock(articleId);
        if (!Integer.valueOf(0).equals(article.getStatus())) throw new BusinessException(SCHEDULE_DRAFT_REQUIRED);
        Db.updateBySql("UPDATE article SET scheduled_publish_at=NULL, scheduled_by=NULL, schedule_error=NULL WHERE id=? AND deleted=0", articleId);
        evictAfterCommit(articleId);
    }

    @Override
    public int publishDue() {
        var due = articles.selectListByQuery(QueryWrapper.create().select(ARTICLE.ID)
                .where(ARTICLE.STATUS.eq(0)).and(ARTICLE.SCHEDULED_PUBLISH_AT.le(LocalDateTime.now(ZONE)))
                .and(ARTICLE.SCHEDULE_ERROR.isNull()).orderBy(ARTICLE.SCHEDULED_PUBLISH_AT.asc(), ARTICLE.ID.asc()).limit(100));
        int published = 0;
        var tx = new TransactionTemplate(transactions);
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        for (Article item : due) {
            try {
                if (Boolean.TRUE.equals(tx.execute(status -> publishOne(item.getId())))) published++;
            } catch (Exception error) {
                // 仅记录文章ID和异常类型，不让一篇文章阻止其余计划；下次轮询会重试。
                log.warn("定时发布暂未完成，articleId={}，原因={}", item.getId(), error.getClass().getSimpleName());
            }
        }
        return published;
    }

    /** 事务内锁定并再次判断到期条件，确保取消、手动发布、删除和多实例轮询互斥。 */
    private boolean publishOne(Long id) {
        Article article = articles.selectOneByQuery(QueryWrapper.create().where(ARTICLE.ID.eq(id)).forUpdate());
        if (article == null || !Integer.valueOf(0).equals(article.getStatus()) || article.getScheduledPublishAt() == null
                || article.getScheduleError() != null || article.getScheduledPublishAt().isAfter(LocalDateTime.now(ZONE))) return false;
        String reason = null;
        if (!publisherStillAllowed(article.getScheduledBy())) reason = "定时发布已暂停：安排者账号或发布权限已变化，请管理员重新安排";
        else if (!activeUser(article.getUserId())) reason = "定时发布已暂停：作者账号已停用或注销";
        else if (!validContent(article)) reason = "定时发布已暂停：文章标题、正文或分类不完整";
        if (reason != null) {
            Db.updateBySql("UPDATE article SET schedule_error=? WHERE id=?", reason, id);
            evictAfterCommit(id);
            return false;
        }
        Db.updateBySql("UPDATE article SET status=1, scheduled_publish_at=NULL, scheduled_by=NULL, schedule_error=NULL, update_time=NOW() WHERE id=? AND deleted=0", id);
        evictAfterCommit(id);
        return true;
    }

    /** 与手动发布相同的入口权限，服务被直接调用时仍不能绕过。 */
    private void requirePublisher() {
        if (SecurityUtils.getLoginUser() == null || !SecurityUtils.isAdmin() || !SecurityUtils.hasPermission("article:update"))
            throw new AccessDeniedException("需要文章管理权限");
    }

    /** 锁定未删除文章；与现有编辑、删除操作使用同一行锁。 */
    private Article lock(Long id) {
        if (id == null || id < 1) throw new BusinessException(ARTICLE_NOT_FOUND);
        Article article = articles.selectOneByQuery(QueryWrapper.create().where(ARTICLE.ID.eq(id)).forUpdate());
        if (article == null) throw new BusinessException(ARTICLE_NOT_FOUND);
        return article;
    }

    /** 从数据库重新检查有效管理员角色和文章修改权限，不借用请求线程身份。 */
    private boolean publisherStillAllowed(Long id) {
        if (!activeUser(id)) return false;
        var roles = Db.selectListBySql("SELECT r.name FROM user_role ur JOIN role r ON r.id=ur.role_id WHERE ur.user_id=? AND r.deleted=0", id);
        if (roles.stream().anyMatch(r -> "SUPER_ADMIN".equals(r.getString("name")))) return true;
        return roles.stream().anyMatch(r -> "ADMIN".equals(r.getString("name"))) &&
                Db.selectOneBySql("SELECT p.id FROM permission p JOIN role_permission rp ON rp.permission_id=p.id JOIN user_role ur ON ur.role_id=rp.role_id JOIN role r ON r.id=ur.role_id WHERE ur.user_id=? AND r.deleted=0 AND p.status=0 AND p.name='article:update' LIMIT 1", id) != null;
    }

    /** 已注销或非正常状态的账号不能安排或触发自动发布。 */
    private boolean activeUser(Long id) {
        return id != null && Db.selectOneBySql("SELECT id FROM user WHERE id=? AND deleted=0 AND status=0", id) != null;
    }

    /** 发布前必须有正文、标题和有效分类；不将半成品草稿自动公开。 */
    private boolean validContent(Article article) {
        return article.getTitle() != null && !article.getTitle().isBlank() && article.getContent() != null && !article.getContent().isBlank()
                && Db.selectOneBySql("SELECT c.id FROM article_category ac JOIN category c ON c.id=ac.category_id WHERE ac.article_id=? AND c.deleted=0 LIMIT 1", article.getId()) != null;
    }

    /** 提交后清理详情缓存，回滚不影响当前缓存。 */
    private void evictAfterCommit(Long id) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { cache.delete(ARTICLE_DETAIL + id); }
        });
    }
}
