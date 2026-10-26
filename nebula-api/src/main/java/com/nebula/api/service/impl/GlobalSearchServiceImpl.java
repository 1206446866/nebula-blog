package com.nebula.api.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.row.RowMapper;
import com.nebula.api.service.GlobalSearchService;
import com.nebula.api.vo.search.*;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.exception.code.SearchErrorCode;
import com.nebula.common.util.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mybatis.spring.SqlSessionTemplate;
import java.util.*;

@Service
public class GlobalSearchServiceImpl implements GlobalSearchService {
    private static final List<String> TYPES = List.of("article", "comment", "user", "category", "tag");
    private final RowMapper queries;

    public GlobalSearchServiceImpl(SqlSessionTemplate sessions) {
        // 使用当前应用上下文的Flex会话，确保每条查询都加入本服务的限时事务。
        this.queries = sessions.getMapper(RowMapper.class);
    }

    @Override
    @Transactional(readOnly = true, timeoutString = "${nebula.resources.search-timeout-seconds:3}")
    public SearchResultVO search(String keyword, String type, int current, int size) {
        if (SecurityUtils.getLoginUser() == null) throw new AccessDeniedException("请先登录");
        if (keyword == null) keyword = "";
        keyword = keyword.strip();
        if (keyword.length() > 50 || current < 1 || size < 1 || size > 50 || (!"all".equals(type) && !TYPES.contains(type)))
            throw new BusinessException(SearchErrorCode.INVALID_SEARCH);
        var counts = new LinkedHashMap<String, Long>();
        TYPES.forEach(t -> counts.put(t, 0L));
        if (keyword.isBlank()) return new SearchResultVO(Page.of(1, size, 0), counts);
        String pattern = "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var sources = sources(pattern);
        for (var entry : sources.entrySet()) {
            var source = entry.getValue();
            counts.put(entry.getKey(), queries.selectOneBySql("SELECT COUNT(*) AS total FROM (" + source.sql + ") hits", source.args.toArray()).getLong("total"));
        }
        List<Source> selected = sources.entrySet().stream().filter(e -> "all".equals(type) || e.getKey().equals(type)).map(Map.Entry::getValue).toList();
        long total = "all".equals(type) ? counts.values().stream().mapToLong(Long::longValue).sum() : counts.get(type);
        long pageNumber = Math.min(current, Math.max(1, (total + size - 1) / size));
        Page<SearchItemVO> page = Page.of(pageNumber, size, total);
        if (total == 0 || selected.isEmpty()) return new SearchResultVO(page, counts);
        String union = String.join(" UNION ALL ", selected.stream().map(this::unionProjection).toList());
        List<Object> args = new ArrayList<>();
        selected.forEach(s -> args.addAll(s.args));
        args.add(size); args.add((pageNumber - 1) * size);
        var rows = queries.selectListBySql("SELECT * FROM (" + union + ") hits ORDER BY occurred_at DESC, kind ASC, id DESC LIMIT ? OFFSET ?", args.toArray());
        page.setRecords(rows.stream().map(row -> new SearchItemVO(row.getString("kind"), row.getLong("id"), row.getLong("target_id"),
                row.getString("title"), row.getString("excerpt"), row.getString("avatar"), row.getString("occurred_at"))).toList());
        return new SearchResultVO(page, counts);
    }

    /** 仅拼接固定SQL片段；关键词全部绑定，并将通配符当作普通文字处理。 */
    private Map<String, Source> sources(String pattern) {
        var sources = new LinkedHashMap<String, Source>();
        if (SecurityUtils.hasPermission("article:list")) {
            sources.put("article", new Source("SELECT 'article' kind,a.id,a.id target_id,a.title,LEFT(a.content,180) excerpt,NULL avatar,a.create_time occurred_at FROM article a WHERE a.deleted=0 AND a.status=1 AND (a.title LIKE ? ESCAPE '!' OR a.content LIKE ? ESCAPE '!' OR a.author LIKE ? ESCAPE '!')", List.of(pattern, pattern, pattern)));
            sources.put("comment", new Source("SELECT 'comment' kind,c.id,c.article_id target_id,a.title,LEFT(c.content,180) excerpt,NULL avatar,c.create_time occurred_at FROM comment c JOIN article a ON a.id=c.article_id WHERE c.status=0 AND a.deleted=0 AND a.status=1 AND c.content LIKE ? ESCAPE '!'", List.of(pattern)));
        }
        sources.put("user", new Source("SELECT 'user' kind,u.id,u.id target_id,u.username title,u.nid excerpt,u.avatar,u.create_time occurred_at FROM user u WHERE u.deleted=0 AND u.status=0 AND (u.username LIKE ? ESCAPE '!' OR u.nid LIKE ? ESCAPE '!')", List.of(pattern, pattern)));
        if (SecurityUtils.hasPermission("category:list"))
            sources.put("category", new Source("SELECT 'category' kind,c.id,c.id target_id,c.name title,LEFT(c.description,180) excerpt,NULL avatar,c.create_time occurred_at FROM category c WHERE c.deleted=0 AND (c.name LIKE ? ESCAPE '!' OR c.description LIKE ? ESCAPE '!')", List.of(pattern, pattern)));
        if (SecurityUtils.hasPermission("tag:list"))
            sources.put("tag", new Source("SELECT 'tag' kind,t.id,t.id target_id,t.name title,LEFT(t.description,180) excerpt,NULL avatar,t.create_time occurred_at FROM tag t WHERE t.deleted=0 AND (t.name LIKE ? ESCAPE '!' OR t.description LIKE ? ESCAPE '!')", List.of(pattern, pattern)));
        return sources;
    }

    /** 兼容历史表的不同字符排序规则，只统一查询结果，避免UNION因排序规则不同失败。 */
    private String unionProjection(Source source) {
        return "SELECT CONVERT(kind USING utf8mb4) COLLATE utf8mb4_unicode_ci AS kind,id,target_id,"
                + "CONVERT(title USING utf8mb4) COLLATE utf8mb4_unicode_ci AS title,"
                + "CONVERT(excerpt USING utf8mb4) COLLATE utf8mb4_unicode_ci AS excerpt,"
                + "CONVERT(avatar USING utf8mb4) COLLATE utf8mb4_unicode_ci AS avatar,occurred_at FROM (" + source.sql + ") search_source";
    }

    private record Source(String sql, List<Object> args) {}
}
