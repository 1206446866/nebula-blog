package com.nebula.api.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.row.RowMapper;
import com.nebula.api.search.SearchHit;
import com.nebula.api.vo.search.*;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.exception.code.SearchErrorCode;
import com.nebula.common.util.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.mybatis.spring.SqlSessionTemplate;
import java.util.*;

/** 公开检索资料与mysql后备查询共用权限、包含匹配及字段投影，防止两条路径的读取边界分叉。 */
@Service
public class SearchSourceService {
    private static final List<String> TYPES = List.of("article", "comment", "user", "category", "tag");
    private final RowMapper queries;

    public SearchSourceService(SqlSessionTemplate sessions) {
        // 使用当前应用上下文的Flex会话，确保每条查询都加入本服务的限时事务。
        this.queries = sessions.getMapper(RowMapper.class);
    }

    /** 仅按有界候选编号回查当前公开资料、权限、原包含规则与同步版本，不采信ES展示字段。 */
    public Map<String, SearchItemVO> visibleMatches(List<SearchHit> hits, String keyword) {
        String pattern = "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        Map<String, SearchItemVO> result = new HashMap<>();
        for (var entry : sources(pattern).entrySet()) {
            var expected = new HashMap<Long, Long>();
            for (var hit : hits) if (hit.kind().equals(entry.getKey())) expected.put(hit.id(), hit.generation());
            if (expected.isEmpty()) continue;
            var parameters = new ArrayList<Object>(entry.getValue().args); parameters.addAll(expected.keySet());
            String tableAlias = alias(entry.getKey());
            String sql = entry.getValue().sql.replaceFirst("SELECT ", "SELECT st.generation search_generation,")
                    .replace(" WHERE ", " LEFT JOIN search_sync_task st ON st.kind='" + entry.getKey() + "' AND st.target_id=" + tableAlias + ".id WHERE ")
                    + " AND " + tableAlias + ".id IN (" + String.join(",", Collections.nCopies(expected.size(), "?")) + ")";
            for (var row : queries.selectListBySql(sql, parameters.toArray())) {
                if (!Objects.equals(expected.get(row.getLong("id")), row.getLong("search_generation"))) continue;
                var item = new SearchItemVO(row.getString("kind"), row.getLong("id"), row.getLong("target_id"), row.getString("title"),
                        row.getString("excerpt"), row.getString("avatar"), row.getString("occurred_at"));
                result.put(item.type() + ":" + item.id(), item);
            }
        }
        return result;
    }

    /** 显式切换mysql后端时使用原有受限查询；正常ES请求只调用有界候选核验。 */
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
        // 命中集合只保存编号和排序时间，同一条SQL中复用；不重复扫描正文，不携带摘要进行大排序。
        String union = String.join(" UNION ALL ", sources.entrySet().stream()
                .map(e -> narrowProjection(e.getKey(), e.getValue())).toList());
        String filter = "all".equals(type) ? "1=1" : "kind=?";
        var rows = queries.selectListBySql("WITH hits AS (" + union + "), counts AS (SELECT kind,COUNT(*) total FROM hits GROUP BY kind), "
                + "ranked AS (SELECT candidates.*,ROW_NUMBER() OVER (ORDER BY occurred_at DESC,kind ASC,id DESC) position_no FROM "
                + "(SELECT * FROM hits WHERE " + filter + " ORDER BY occurred_at DESC,kind ASC,id DESC LIMIT ?) candidates), "
                + "bounds AS (SELECT (LEAST(?,GREATEST(1,CEIL(COALESCE(SUM(total),0)/?)))-1)*? first_row FROM counts WHERE " + filter + ") "
                + "SELECT 'count' row_kind,kind,total,NULL id,NULL target_id,NULL occurred_at,0 position_no FROM counts "
                + "UNION ALL SELECT 'item',kind,0,id,target_id,occurred_at,position_no FROM ranked CROSS JOIN bounds "
                + "WHERE position_no>first_row AND position_no<=first_row+?", searchArguments(sources, type, current, size));
        for (var row : rows) if ("count".equals(row.getString("row_kind"))) counts.put(row.getString("kind"), row.getLong("total"));
        long total = "all".equals(type) ? counts.values().stream().mapToLong(Long::longValue).sum() : counts.get(type);
        long pageNumber = Math.min(current, Math.max(1, (total + size - 1) / size));
        Page<SearchItemVO> page = Page.of(pageNumber, size, total);
        var items = rows.stream().filter(r -> "item".equals(r.getString("row_kind")))
                .sorted(Comparator.comparingLong(r -> r.getLong("position_no"))).toList();
        Map<String, SearchItemVO> details = new HashMap<>();
        for (var entry : sources.entrySet()) {
            var ids = items.stream().filter(r -> entry.getKey().equals(r.getString("kind"))).map(r -> r.getLong("id")).toList();
            if (ids.isEmpty()) continue;
            var parameters = new ArrayList<Object>(entry.getValue().args); parameters.addAll(ids);
            String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
            for (var row : queries.selectListBySql(entry.getValue().sql + " AND " + alias(entry.getKey()) + ".id IN (" + placeholders + ")", parameters.toArray())) {
                var item = new SearchItemVO(row.getString("kind"), row.getLong("id"), row.getLong("target_id"), row.getString("title"),
                        row.getString("excerpt"), row.getString("avatar"), row.getString("occurred_at"));
                details.put(item.type() + ":" + item.id(), item);
            }
        }
        page.setRecords(items.stream().map(r -> details.get(r.getString("kind") + ":" + r.getLong("id"))).toList());
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

    /** 各表仅返回排序所需列，避免不同正文排序规则参与联合查询。 */
    private String narrowProjection(String kind, Source source) {
        String a = alias(kind);
        // 只物化公开文章的编号与状态，避免评论命中逐条回查含大正文的文章行。
        String from = source.sql.substring(source.sql.indexOf(" FROM "));
        if ("comment".equals(kind)) from = from.replace("JOIN article a ON",
                "JOIN (SELECT DISTINCT id,deleted,status FROM article WHERE deleted=0 AND status=1) a ON");
        return "SELECT CONVERT('" + kind + "' USING utf8mb4) COLLATE utf8mb4_unicode_ci kind," + a + ".id,"
                + ("comment".equals(kind) ? "c.article_id" : a + ".id") + " target_id," + a + ".create_time occurred_at"
                + from;
    }

    /** 表别名只从固定类型映射取得，不接受客户端提供的SQL标识符。 */
    private String alias(String kind) {
        return switch (kind) { case "article" -> "a"; case "user" -> "u"; case "tag" -> "t"; default -> "c"; };
    }

    /** 参数按命中集合、所选类型、页码边界的占位顺序绑定。 */
    private Object[] searchArguments(Map<String, Source> sources, String type, int current, int size) {
        var args = new ArrayList<Object>(); sources.values().forEach(s -> args.addAll(s.args));
        if (!"all".equals(type)) args.add(type);
        args.add((long) current * size);
        args.add(current); args.add(size); args.add(size);
        if (!"all".equals(type)) args.add(type);
        args.add(size); return args.toArray();
    }

    private record Source(String sql, List<Object> args) {}
}
