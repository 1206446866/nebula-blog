package com.nebula.media.service;

import com.mybatisflex.core.row.RowMapper;
import com.nebula.common.exception.BusinessException;
import com.nebula.media.MediaError;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;

/** 由数据库中的账号行串行保护配额检查，与媒体记录写入处于同一事务。 */
@Service
public class MediaQuotaService {
    private final MediaLimitProperties limits;
    private final RowMapper queries;

    public MediaQuotaService(SqlSessionTemplate sessions, MediaLimitProperties limits) {
        this.limits = limits;
        // 与媒体Mapper共享当前上下文和事务，账号行锁持续到媒体记录提交或回滚。
        this.queries = sessions.getMapper(RowMapper.class);
    }

    /**
     * 在落盘前检查账号累计和当日上传额度，持有账号锁直到外层上传事务结束。
     * @param userId 已获准上传的资源归属账号
     * @param bytes 即将保存的准确文件字节数
     * @throws BusinessException 账号不可用、文件非法或配额不足时拒绝上传
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void check(Long userId, long bytes) {
        if (bytes < 1 || bytes > ImageStorage.MAX_SIZE) throw new BusinessException(MediaError.INVALID_IMAGE);
        if (queries.selectOneBySql("SELECT id FROM user WHERE id=? AND deleted=0 AND status=0 FOR UPDATE", userId) == null)
            throw new BusinessException(MediaError.NOT_FOUND);
        var start = LocalDate.now(ZoneId.of("Asia/Shanghai")).atStartOfDay();
        // 使用锁定读取得最新已提交记录，避免REPEATABLE READ旧快照低估并发上传占用。
        var usage = queries.selectOneBySql("""
                SELECT COALESCE(SUM(CASE WHEN deleted=0 THEN size ELSE 0 END),0) AS used_bytes,
                       COALESCE(SUM(CASE WHEN deleted=0 THEN 1 ELSE 0 END),0) AS used_files,
                       COALESCE(SUM(CASE WHEN create_time>=? THEN size ELSE 0 END),0) AS day_bytes,
                       COALESCE(SUM(CASE WHEN create_time>=? THEN 1 ELSE 0 END),0) AS day_files
                FROM media_asset WHERE user_id=? FOR UPDATE
                """, start, start, userId);
        if (bytes > limits.getAccountBytes() - usage.getLong("used_bytes") || usage.getLong("used_files") >= limits.getAccountFiles())
            throw new BusinessException(MediaError.ACCOUNT_QUOTA);
        if (bytes > limits.getDailyBytes() - usage.getLong("day_bytes") || usage.getLong("day_files") >= limits.getDailyFiles())
            throw new BusinessException(MediaError.DAILY_QUOTA);
    }
}
