package com.nebula.article.service;

import com.mybatisflex.core.row.Db;
import com.nebula.common.util.SecurityUtils;
import com.nebula.media.service.MediaReferences;
import org.springframework.stereotype.Component;
import java.util.List;

/** 历史正文中的图片仍受引用保护，但历史版本不会赋予公开读取权限。 */
@Component
public class RevisionMediaPolicy implements MediaReferences {
    @Override
    public List<Reference> find(String path) {
        return Db.selectListBySql("SELECT r.article_id,r.payload,a.user_id FROM article_revision r JOIN article a ON a.id=r.article_id AND a.deleted=0 WHERE r.payload LIKE ?", "%" + path + "%")
                .stream().filter(row -> row.getString("payload").contains(path))
                .map(row -> new Reference("revision", row.getLong("article_id"), "文章历史版本", false,
                        SecurityUtils.hasPermission("article:update") && (SecurityUtils.isAdmin() || SecurityUtils.getLoginUser() != null && SecurityUtils.isSelf(row.getLong("user_id")))))
                .toList();
    }
}
