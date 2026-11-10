package com.nebula.article.service.impl;

import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.nebula.article.dto.ArticleSeriesDto;
import com.nebula.article.vo.ArticleSeriesVO;
import com.nebula.article.service.ArticleSeriesService;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.util.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.nebula.common.exception.code.ArticleErrorCode.*;

@Service
@lombok.RequiredArgsConstructor
public class ArticleSeriesServiceImpl implements ArticleSeriesService {
    private final com.nebula.article.service.ArticlePrivateDataService privateData;
    private static final String COUNT = "(SELECT COUNT(*) FROM article_series_item i JOIN article a ON a.id=i.article_id AND a.deleted=0 AND a.status=1 WHERE i.series_id=s.id) article_count";
    @Override
    public ArticleSeriesVO.Listing list(boolean mine, int current) {
        requireReader();
        if (current < 1 || current > 10000) throw new BusinessException(ARTICLE_SERIES_INVALID);
        String filter = mine ? "s.user_id=?" : "s.public_visible=1";
        Object[] args = mine ? new Object[]{SecurityUtils.getUserId()} : new Object[]{};
        long total = Db.selectOneBySql("SELECT COUNT(*) total FROM article_series s WHERE " + filter, args).getLong("total");
        var params = new ArrayList<>(Arrays.asList(args)); params.add((long) (current - 1) * 20);
        var records = Db.selectListBySql("SELECT s.*," + COUNT + " FROM article_series s WHERE " + filter + " ORDER BY s.updated_at DESC,s.id DESC LIMIT 20 OFFSET ?", params.toArray());
        return new ArticleSeriesVO.Listing(records.stream().map(this::summary).toList(), total, current, 20);
    }

    @Override
    public ArticleSeriesVO.Detail get(long id, boolean editing) {
        requireReader(); Row series = requireSeries(id, false);
        if (editing) requireEditor(series);
        else if (series.getInt("public_visible") != 1 && !SecurityUtils.isSelf(series.getLong("user_id")) && !SecurityUtils.isAdmin()) throw new BusinessException(ARTICLE_NOT_FOUND);
        var articles = Db.selectListBySql("SELECT a.id,a.title,a.status FROM article_series_item i JOIN article a ON a.id=i.article_id AND a.deleted=0 WHERE i.series_id=?"
                + (editing ? "" : " AND a.status=1") + " ORDER BY i.position", id)
                .stream().map(row -> new ArticleSeriesVO.Item(row.getLong("id"), row.getString("title"), row.getInt("status"))).toList();
        return new ArticleSeriesVO.Detail(new ArticleSeriesVO.Summary(id, series.getLong("user_id"), series.getString("title"), series.getString("description"),
                series.getInt("public_visible") == 1, articles.size()), articles);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(Long id, ArticleSeriesDto dto) {
        requireReader();
        if (!SecurityUtils.hasPermission("article:update")) throw new AccessDeniedException("需要文章编辑权限");
        privateData.lockActiveAccount(SecurityUtils.getUserId());
        long owner = SecurityUtils.getUserId();
        if (id != null) { Row row = requireSeries(id, true); requireEditor(row); owner = row.getLong("user_id"); }
        // 对文章按编号加锁，避免两个系列以不同排序保存时相互等待。
        var ids = dto.articleIds().stream().distinct().sorted().toList();
        if (ids.size() != dto.articleIds().size()) throw new BusinessException(ARTICLE_SERIES_INVALID);
        for (long articleId : ids) {
            var row = Db.selectOneBySql("SELECT user_id FROM article WHERE id=? AND deleted=0 FOR UPDATE", articleId);
            if (row == null || row.getLong("user_id") != owner) throw new BusinessException(ARTICLE_SERIES_INVALID);
        }
        if (id == null) {
            Db.selectOneBySql("SELECT id FROM user WHERE id=? FOR UPDATE", owner);
            if (Db.selectOneBySql("SELECT COUNT(*) total FROM article_series WHERE user_id=?", owner).getLong("total") >= 100)
                throw new BusinessException(ARTICLE_SERIES_LIMIT);
            Row row = Row.ofKey(com.mybatisflex.core.row.RowKey.AUTO); row.set("user_id", owner); row.set("title", dto.title().strip()); row.set("description", dto.description()); row.set("public_visible", dto.publicVisible());
            Db.insert("article_series", row); id = row.getLong("id");
        } else {
            Db.updateBySql("UPDATE article_series SET title=?,description=?,public_visible=?,updated_at=NOW(3) WHERE id=?", dto.title().strip(), dto.description(), dto.publicVisible(), id);
        }
        Db.deleteBySql("DELETE FROM article_series_item WHERE series_id=?", id);
        int position = 0;
        for (long articleId : dto.articleIds()) Db.updateBySql("INSERT INTO article_series_item(series_id,article_id,position) VALUES(?,?,?)", id, articleId, position++);
        return id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(long id) {
        requireReader(); requireEditor(requireSeries(id, true));
        Db.deleteBySql("DELETE FROM article_series_item WHERE series_id=?", id);
        return Db.deleteBySql("DELETE FROM article_series WHERE id=?", id) == 1;
    }

    @Override
    public List<ArticleSeriesVO.Summary> forArticle(long articleId) {
        requireReader();
        return Db.selectListBySql("SELECT s.*," + COUNT + " FROM article_series s JOIN article_series_item i ON i.series_id=s.id JOIN article a ON a.id=i.article_id AND a.deleted=0 AND a.status=1 WHERE i.article_id=? AND s.public_visible=1 ORDER BY s.updated_at DESC,s.id DESC LIMIT 20", articleId)
                .stream().map(this::summary).toList();
    }

    /** 系列沿用文章阅读权限，避免绕过账号权限读取文章标题。 */
    private void requireReader() {
        if (SecurityUtils.getLoginUser() == null || !SecurityUtils.hasPermission("article:list")) throw new AccessDeniedException("需要文章阅读权限");
    }
    /** 管理员仍需文章编辑权限，作者不能操作他人的系列。 */
    private void requireEditor(Row row) {
        if (!SecurityUtils.hasPermission("article:update") || !SecurityUtils.isAdmin() && !SecurityUtils.isSelf(row.getLong("user_id"))) throw new BusinessException(ARTICLE_ACCESS_DENIED);
    }
    /** 查询存在的系列，写操作先锁定整组编排。 */
    private Row requireSeries(long id, boolean lock) {
        Row row = Db.selectOneBySql("SELECT * FROM article_series WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
        if (row == null) throw new BusinessException(ARTICLE_NOT_FOUND);
        return row;
    }
    /** 公开摘要只计算当前已发布且未删除的文章。 */
    private ArticleSeriesVO.Summary summary(Row row) {
        return new ArticleSeriesVO.Summary(row.getLong("id"), row.getLong("user_id"), row.getString("title"), row.getString("description"), row.getInt("public_visible") == 1, row.getInt("article_count"));
    }
}
