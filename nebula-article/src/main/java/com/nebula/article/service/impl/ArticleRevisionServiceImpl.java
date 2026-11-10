package com.nebula.article.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.row.Db;
import com.nebula.article.dto.WorkspaceDto;
import com.nebula.article.entity.Article;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.article.service.ArticleRevisionService;
import com.nebula.article.vo.ArticleRevisionVO;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.util.List;
import static com.nebula.common.exception.code.ArticleErrorCode.*;

@Service
@RequiredArgsConstructor
public class ArticleRevisionServiceImpl implements ArticleRevisionService {
    private final ArticleMapper articles;
    private final ObjectMapper json;

    @Override
    public void capture(Article article) {
        long version = article.getEditVersion() == null ? 0 : article.getEditVersion();
        if (Db.selectOneBySql("SELECT id FROM article_revision WHERE article_id=? AND edit_version=?", article.getId(), version) != null) return;
        var category = Db.selectOneBySql("SELECT category_id FROM article_category WHERE article_id=? ORDER BY category_id LIMIT 1", article.getId());
        var tags = Db.selectListBySql("SELECT tag_id FROM article_tag WHERE article_id=? ORDER BY tag_id", article.getId())
                .stream().map(row -> row.getLong("tag_id")).toList();
        var draft = new WorkspaceDto.Draft(article.getTitle(), article.getContent(), category == null ? null : category.getLong("category_id"), tags, version);
        String payload;
        try { payload = json.writeValueAsString(draft); }
        catch (JsonProcessingException e) { throw new IllegalStateException("文章版本序列化失败", e); }
        Db.updateBySql("INSERT INTO article_revision(article_id,edit_version,editor_id,title,payload) VALUES(?,?,?,?,?)",
                article.getId(), version, SecurityUtils.getUserId(), article.getTitle(), payload);
        Db.deleteBySql("DELETE FROM article_revision WHERE article_id=? AND id NOT IN "
                + "(SELECT id FROM (SELECT id FROM article_revision WHERE article_id=? ORDER BY edit_version DESC LIMIT 30) retained)", article.getId(), article.getId());
    }

    @Override
    public List<ArticleRevisionVO.Summary> list(long articleId) {
        requireEditor(articleId);
        return Db.selectListBySql("SELECT id,edit_version,title,created_at FROM article_revision WHERE article_id=? ORDER BY edit_version DESC LIMIT 30", articleId)
                .stream().map(row -> new ArticleRevisionVO.Summary(row.getLong("id"), row.getLong("edit_version"), row.getString("title"), row.getString("created_at"))).toList();
    }

    @Override
    public ArticleRevisionVO.Detail get(long articleId, long revisionId) {
        requireEditor(articleId);
        var row = Db.selectOneBySql("SELECT id,edit_version,payload FROM article_revision WHERE article_id=? AND id=?", articleId, revisionId);
        if (row == null) throw new BusinessException(ARTICLE_NOT_FOUND);
        try { return new ArticleRevisionVO.Detail(row.getLong("id"), row.getLong("edit_version"), json.readValue(row.getString("payload"), WorkspaceDto.Draft.class)); }
        catch (JsonProcessingException e) { throw new IllegalStateException("文章版本读取失败", e); }
    }

    /** 历史版本可能包含已撤下内容，只允许当前文章的编辑者访问。 */
    private void requireEditor(long articleId) {
        if (SecurityUtils.getLoginUser() == null || !SecurityUtils.hasPermission("article:update")) throw new AccessDeniedException("需要文章编辑权限");
        Article article = articles.selectOneById(articleId);
        if (article == null) throw new BusinessException(ARTICLE_NOT_FOUND);
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isSelf(article.getUserId())) throw new BusinessException(ARTICLE_ACCESS_DENIED);
    }
}
