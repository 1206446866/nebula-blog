package com.nebula.article.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import com.nebula.article.dto.WorkspaceDto;
import com.nebula.article.service.ArticleService;
import com.nebula.article.service.ArticleWorkspaceService;
import com.nebula.article.vo.WorkspaceVO;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.util.SecurityUtils;
import com.nebula.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.nebula.common.exception.code.ArticleErrorCode.*;

@Service
@RequiredArgsConstructor
public class ArticleWorkspaceServiceImpl implements ArticleWorkspaceService {
    private final ArticleService articles;
    private final MediaService media;
    private final ObjectMapper json;
    private final com.nebula.article.service.ArticlePrivateDataService privateData;

    @Override
    @Transactional(readOnly = true)
    public WorkspaceVO get(String key) {
        long owner = requireOwner(key);
        return view(Db.selectOneBySql("SELECT *, updated_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) AS active "
                + "FROM article_workspace WHERE user_id=? AND document_key=?", owner, key));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceVO save(String key, WorkspaceDto dto) {
        long owner = requireOwner(key);
        privateData.lockActiveAccount(owner);
        if (dto.draft() != null) media.lockArticleImages(dto.draft().content());
        // 同一账号串行创建记录，同时限制备份数量；空记录保留版本，防止旧请求复活已清除内容。
        Db.updateBySql("UPDATE article_workspace SET payload=NULL WHERE user_id=? AND payload IS NOT NULL "
                + "AND updated_at < DATE_SUB(NOW(), INTERVAL 30 DAY)", owner);
        Row old = Db.selectOneBySql("SELECT revision FROM article_workspace WHERE user_id=? AND document_key=? FOR UPDATE", owner, key);
        long revision = old == null ? 0 : old.getLong("revision");
        if (revision != dto.revision()) throw new BusinessException(ARTICLE_EDIT_CONFLICT);
        if (old == null) {
            long count = Db.selectOneBySql("SELECT COUNT(*) AS total FROM article_workspace WHERE user_id=?", owner).getLong("total");
            if (count >= 1000) throw new BusinessException(ARTICLE_WORKSPACE_LIMIT);
            Db.updateBySql("INSERT INTO article_workspace(user_id,document_key,revision,payload) VALUES(?,?,1,?)", owner, key, encode(dto.draft()));
        } else {
            Db.updateBySql("UPDATE article_workspace SET revision=revision+1,payload=?,updated_at=NOW(3) WHERE user_id=? AND document_key=?",
                    encode(dto.draft()), owner, key);
        }
        return get(key);
    }

    /** 备份只属于当前账号；管理员编辑他人文章时也只能读取自己的备份。 */
    private long requireOwner(String key) {
        if (SecurityUtils.getLoginUser() == null) throw new AccessDeniedException("请先登录");
        if ("new".equals(key)) {
            if (!SecurityUtils.hasPermission("article:create")) throw new AccessDeniedException("需要文章创建权限");
        } else if (key != null && key.matches("article-[1-9][0-9]{0,17}")) {
            articles.getArticleForEdit(Long.parseLong(key.substring(8)));
        } else throw new BusinessException(ARTICLE_WORKSPACE_INVALID);
        return SecurityUtils.getUserId();
    }

    /** 过期备份不再恢复，版本号保留以阻止过期客户端覆盖。 */
    private WorkspaceVO view(Row row) {
        if (row == null) return new WorkspaceVO(0, null, null);
        String payload = row.getString("payload");
        WorkspaceDto.Draft draft = null;
        if (payload != null && row.getInt("active") == 1) {
            try { draft = json.readValue(payload, WorkspaceDto.Draft.class); }
            catch (JsonProcessingException e) { throw new IllegalStateException("编辑备份读取失败", e); }
        }
        return new WorkspaceVO(row.getLong("revision"), draft, row.getString("updated_at"));
    }

    /** 使用固定结构保存内容，禁止将请求中的任意字段写入备份。 */
    private String encode(WorkspaceDto.Draft draft) {
        if (draft == null) return null;
        try { return json.writeValueAsString(draft); }
        catch (JsonProcessingException e) { throw new IllegalStateException("编辑备份序列化失败", e); }
    }
}
