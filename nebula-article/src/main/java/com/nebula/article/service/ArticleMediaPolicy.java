package com.nebula.article.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.common.util.SecurityUtils;
import com.nebula.media.service.MediaReferences;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import static com.nebula.article.entity.table.ArticleTableDef.ARTICLE;

@Component
@RequiredArgsConstructor
public class ArticleMediaPolicy implements MediaReferences {
    private final ArticleMapper articles;
    @Override public List<Reference> find(String path) {
        // Conservatively protect every textual reference, including Markdown reference links.
        return articles.selectListByQuery(QueryWrapper.create().where(ARTICLE.CONTENT.like(path))).stream()
                .filter(article -> article.getContent() != null && article.getContent().contains(path))
                .map(article -> {
                    boolean published = Integer.valueOf(1).equals(article.getStatus());
                    boolean readable = (SecurityUtils.hasPermission("article:list") && published)
                            || ((SecurityUtils.hasPermission("article:list") || SecurityUtils.hasPermission("article:update"))
                            && (SecurityUtils.isAdmin() || SecurityUtils.getLoginUser() != null && SecurityUtils.isSelf(article.getUserId())));
                    return new Reference("article", article.getId(), article.getTitle(), published, readable);
                }).toList();
    }
}
