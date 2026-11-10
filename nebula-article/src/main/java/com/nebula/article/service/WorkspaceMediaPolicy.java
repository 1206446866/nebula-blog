package com.nebula.article.service;

import com.mybatisflex.core.row.Db;
import com.nebula.common.util.SecurityUtils;
import com.nebula.media.service.MediaReferences;
import org.springframework.stereotype.Component;
import java.util.List;

/** 编辑备份保护其中的图片，但不会把私人图片变成公开资源。 */
@Component
public class WorkspaceMediaPolicy implements MediaReferences {
    @Override
    public List<Reference> find(String path) {
        return Db.selectListBySql("SELECT user_id,document_key,payload FROM article_workspace "
                        + "WHERE payload IS NOT NULL AND updated_at >= DATE_SUB(NOW(),INTERVAL 30 DAY) AND payload LIKE ?", "%" + path + "%")
                .stream().filter(row -> row.getString("payload").contains(path))
                .map(row -> new Reference("workspace", null, "私人编辑备份", false,
                        SecurityUtils.getLoginUser() != null && SecurityUtils.isSelf(row.getLong("user_id"))))
                .toList();
    }
}
