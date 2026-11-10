package com.nebula.article.service.impl;

import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.nebula.article.dto.ReadingDto;
import com.nebula.article.service.ArticleReadingService;
import com.nebula.article.vo.ReadingVO;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.util.PageLimits;
import com.nebula.common.util.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.nebula.common.exception.code.ArticleErrorCode.*;

@Service
@lombok.RequiredArgsConstructor
public class ArticleReadingServiceImpl implements ArticleReadingService {
    private final com.nebula.article.service.ArticlePrivateDataService privateData;
    @Override
    public ReadingVO.State get(long articleId) {
        long owner = owner(); requirePublished(articleId, false);
        return state(Db.selectOneBySql("SELECT * FROM article_reading WHERE user_id=? AND article_id=?", owner, articleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReadingVO.State flags(long articleId, ReadingDto.Flags flags) {
        long owner = owner(); privateData.lockActiveAccount(owner); requirePublished(articleId, true); ensure(owner, articleId);
        Db.updateBySql("UPDATE article_reading SET favorite=?,later=?,updated_at=NOW(3) WHERE user_id=? AND article_id=?",
                flags.favorite(), flags.later(), owner, articleId);
        return get(articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReadingVO.State progress(long articleId, ReadingDto.Progress progress) {
        long owner = owner(); privateData.lockActiveAccount(owner); requirePublished(articleId, true); ensure(owner, articleId);
        Db.updateBySql("UPDATE article_reading SET progress=?,anchor=?,read_at=NOW(3) WHERE user_id=? AND article_id=?",
                progress.progress(), progress.anchor(), owner, articleId);
        // 未收藏的历史最多保留最近1000条，避免持续阅读无限积累数据。
        Db.deleteBySql("DELETE FROM article_reading WHERE user_id=? AND favorite=0 AND later=0 AND article_id NOT IN "
                + "(SELECT article_id FROM (SELECT article_id FROM article_reading WHERE user_id=? ORDER BY read_at DESC,article_id DESC LIMIT 1000) recent)", owner, owner);
        return get(articleId);
    }

    @Override
    public ReadingVO.Library library(String kind, int current, int size) {
        long owner = owner(); PageLimits.requireValid(current, size);
        String filter = switch (kind) {
            case "favorite" -> "r.favorite=1";
            case "later" -> "r.later=1";
            case "history" -> "r.read_at IS NOT NULL";
            default -> throw new BusinessException(ARTICLE_WORKSPACE_INVALID);
        };
        String from = " FROM article_reading r JOIN article a ON a.id=r.article_id AND a.deleted=0 AND a.status=1 WHERE r.user_id=? AND " + filter;
        long total = Db.selectOneBySql("SELECT COUNT(*) total" + from, owner).getLong("total");
        String order = "history".equals(kind) ? "r.read_at" : "r.updated_at";
        var rows = Db.selectListBySql("SELECT r.*,a.title,a.author" + from + " ORDER BY " + order + " DESC,r.article_id DESC LIMIT ? OFFSET ?",
                owner, size, (long) (current - 1) * size);
        return new ReadingVO.Library(rows.stream().map(row -> new ReadingVO.Item(row.getLong("article_id"), row.getString("title"),
                row.getString("author"), state(row))).toList(), total, current, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean clearHistory() {
        long owner = owner();
        Db.updateBySql("UPDATE article_reading SET read_at=NULL,progress=0,anchor='' WHERE user_id=?", owner);
        Db.deleteBySql("DELETE FROM article_reading WHERE user_id=? AND favorite=0 AND later=0", owner);
        return true;
    }

    /** 阅读数据属于当前账号，文章查看权限被撤销时也不返回历史中的标题。 */
    private long owner() {
        if (SecurityUtils.getLoginUser() == null || !SecurityUtils.hasPermission("article:list"))
            throw new AccessDeniedException("需要登录并具有文章阅读权限");
        return SecurityUtils.getUserId();
    }

    /** 写入时锁定文章，与撤回和删除互斥，草稿不能加入阅读库。 */
    private void requirePublished(long id, boolean lock) {
        if (id < 1 || Db.selectOneBySql("SELECT id FROM article WHERE id=? AND deleted=0 AND status=1" + (lock ? " FOR UPDATE" : ""), id) == null)
            throw new BusinessException(ARTICLE_NOT_FOUND);
    }

    /** 当前账号内串行创建并限制总条数，重试不会重复插入。 */
    private void ensure(long owner, long articleId) {
        if (Db.selectOneBySql("SELECT article_id FROM article_reading WHERE user_id=? AND article_id=?", owner, articleId) != null) return;
        if (Db.selectOneBySql("SELECT COUNT(*) total FROM article_reading WHERE user_id=?", owner).getLong("total") >= 10000)
            throw new BusinessException(ARTICLE_READING_LIMIT);
        Db.updateBySql("INSERT INTO article_reading(user_id,article_id) VALUES(?,?)", owner, articleId);
    }

    /** 将数据库行转换为固定结构，未产生记录时返回空白状态。 */
    private ReadingVO.State state(Row row) {
        return row == null ? new ReadingVO.State(false, false, 0, "", null)
                : new ReadingVO.State(row.getInt("favorite") == 1, row.getInt("later") == 1,
                row.getInt("progress"), row.getString("anchor"), row.getString("read_at"));
    }
}
