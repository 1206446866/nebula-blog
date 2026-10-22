package com.nebula.article.service;

import com.mybatisflex.core.row.Db;
import com.nebula.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static com.nebula.common.exception.code.ManagementErrorCode.*;

/** Catalog row locks serialize article bindings with catalog deletion. */
@Component
public class ArticleCatalogReferences {
    public void lockCategory(Long categoryId) {
        if (categoryId == null || categoryId < 1 || Db.selectOneBySql(
                "SELECT id FROM category WHERE id=? AND deleted=0 FOR UPDATE", categoryId) == null) {
            throw new BusinessException(CATEGORY_INVALID);
        }
    }

    public List<Long> lockTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return List.of();
        if (tagIds.size() > 100 || tagIds.stream().anyMatch(id -> id == null || id < 1)) {
            throw new BusinessException(TAG_INVALID);
        }
        var ids = tagIds.stream().distinct().sorted().toList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        var rows = Db.selectListBySql("SELECT id FROM tag WHERE deleted=0 AND id IN (" + placeholders
                + ") ORDER BY id FOR UPDATE", ids.toArray());
        if (rows.size() != ids.size()) throw new BusinessException(TAG_INVALID);
        return ids;
    }
}
