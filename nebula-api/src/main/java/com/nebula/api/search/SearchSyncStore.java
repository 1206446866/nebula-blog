package com.nebula.api.search;

import com.mybatisflex.core.row.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

/** MySQL中的持久同步任务及索引代次；业务事务通过触发器记录变化，ES不可用时仍保留任务。 */
@Repository
public class SearchSyncStore {
    private final RowMapper queries;
    public SearchSyncStore(SqlSessionTemplate sessions) { queries = sessions.getMapper(RowMapper.class); }

    public record Task(String kind, long id) {}
    public record Document(String kind, long id, long generation, Map<String, Object> body) {
        public String key() { return kind + ":" + id; }
    }

    /** 获取持久索引状态，结构迁移未执行时由上层保持搜索不可用。 */
    public Row state() { return one("SELECT * FROM search_index_state WHERE id=1"); }

    /** 利用dirty索引检查是否仍有未同步业务变化；延迟重试的任务也不能视为已追平。 */
    public boolean hasPending() { return one("SELECT target_id FROM search_sync_task WHERE dirty=1 LIMIT 1") != null; }

    /** 聚合尚未完成任务的失败数量与分类，管理入口不暴露正文和引擎原始错误。 */
    public Row failures(long epoch) {
        return one("SELECT COUNT(*) failed_tasks,MAX(last_error) task_error FROM search_sync_task WHERE last_error IS NOT NULL AND (dirty=1 OR applied_epoch<>?)", epoch);
    }

    /** 按重试时刻获取有界任务；已就绪索引只查dirty索引，避免每秒扫描全部任务。 */
    public List<Task> pending(long epoch, boolean ready, int limit) {
        String predicate = ready ? "dirty=1" : "(dirty=1 OR applied_epoch<>" + epoch + ")";
        return queries.selectListBySql("SELECT kind,target_id FROM search_sync_task WHERE " + predicate
                + " AND next_attempt_at<=CURRENT_TIMESTAMP(6) ORDER BY next_attempt_at,updated_at,kind,target_id LIMIT ?", limit)
                .stream().map(r -> new Task(r.getString("kind"), r.getLong("target_id"))).toList();
    }

    /** 在同一个MySQL快照中读取当前版本及公开检索字段，草稿和已删除内容只生成空墓碑。 */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, timeout = 3)
    public Document snapshot(Task task) {
        Row queued = one("SELECT generation FROM search_sync_task WHERE kind=? AND target_id=?", task.kind(), task.id());
        if (queued == null) throw new IllegalStateException("同步任务不存在");
        String sql = switch (task.kind()) {
            case "article" -> "SELECT create_time occurred_at,title,content,author FROM article WHERE id=? AND deleted=0 AND status=1";
            case "comment" -> "SELECT c.create_time occurred_at,c.content FROM comment c JOIN article a ON a.id=c.article_id WHERE c.id=? AND c.status=0 AND a.deleted=0 AND a.status=1";
            case "user" -> "SELECT create_time occurred_at,username,nid FROM user WHERE id=? AND deleted=0 AND status=0";
            case "category" -> "SELECT create_time occurred_at,name,description FROM category WHERE id=? AND deleted=0";
            case "tag" -> "SELECT create_time occurred_at,name,description FROM tag WHERE id=? AND deleted=0";
            default -> throw new IllegalArgumentException("未知同步类型");
        };
        Row source = one(sql, task.id());
        var body = new LinkedHashMap<String, Object>();
        body.put("kind", task.kind()); body.put("id", task.id()); body.put("live", source != null);
        long occurred = 0;
        var text = new ArrayList<String>();
        if (source != null) {
            Object time = source.get("occurred_at");
            if (time != null) occurred = LocalDateTime.parse(time.toString().replace(' ', 'T')).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
            for (var entry : source.entrySet()) if (!entry.getKey().equals("occurred_at"))
                text.add(SearchText.fold(entry.getValue() == null ? "" : entry.getValue().toString()));
        }
        body.put("occurred_at", occurred); body.put("text", text);
        return new Document(task.kind(), task.id(), queued.getLong("generation"), body);
    }

    /** CAS确认已送版本；文章状态或内容变化后，分批持久安排评论重建，不能在业务保存时全量扇出。 */
    @Transactional(timeout = 3)
    public void acknowledge(Document doc, long epoch) {
        Row row = one("SELECT * FROM search_sync_task WHERE kind=? AND target_id=? FOR UPDATE", doc.kind(), doc.id());
        if (row == null || row.getLong("generation") != doc.generation()) return;
        if (state().getLong("epoch") != epoch) return;
        boolean done = !doc.kind().equals("article") || row.getInt("cascade_done") == 1;
        long cursor = row.getLong("cascade_cursor");
        if (!done) {
            var children = queries.selectListBySql("SELECT id FROM comment WHERE article_id=? AND id>? ORDER BY id LIMIT 200", doc.id(), cursor);
            for (Row child : children) {
                cursor = child.getLong("id");
                queries.insertBySql("INSERT INTO search_sync_task(kind,target_id) VALUES('comment',?) ON DUPLICATE KEY UPDATE generation=generation+1,dirty=1,next_attempt_at=CURRENT_TIMESTAMP(6)", cursor);
            }
            done = children.size() < 200;
        }
        queries.updateBySql("UPDATE search_sync_task SET applied_generation=?,applied_epoch=?,dirty=?,cascade_cursor=?,cascade_done=?,attempts=0,last_error=NULL,next_attempt_at=CURRENT_TIMESTAMP(6) WHERE kind=? AND target_id=? AND generation=?",
                doc.generation(), epoch, done ? 0 : 1, cursor, done ? 1 : 0, doc.kind(), doc.id(), doc.generation());
    }

    /** 失败版本延迟重试，后续业务写入会立即重置等待；不持久化正文、凭据或服务端错误详情。 */
    public void failed(Document doc, String code) {
        queries.updateBySql("UPDATE search_sync_task SET attempts=LEAST(attempts+1,10),last_error=?,next_attempt_at=DATE_ADD(CURRENT_TIMESTAMP(6),INTERVAL LEAST(60,POW(2,LEAST(attempts+1,6))) SECOND) WHERE kind=? AND target_id=? AND generation=?",
                code, doc.kind(), doc.id(), doc.generation());
    }

    /** 标记全量重建请求，当前构建期间重复请求合并，业务表和原索引均不删除。 */
    @Transactional(timeout = 3)
    public void requestRebuild() {
        queries.updateBySql("UPDATE search_index_state SET rebuild_requested=1 WHERE id=1 AND (ready=1 OR index_name IS NULL)");
    }

    /** 仅安装仍属于当前代次的新索引，CAS失败的新候选由调用方清理。 */
    @Transactional(timeout = 3)
    public boolean install(long expected, String index) {
        return queries.updateBySql("UPDATE search_index_state SET retired_index=previous_index,previous_index=index_name,index_name=?,epoch=epoch+1,ready=0,rebuild_requested=0,last_error=NULL WHERE id=1 AND epoch=?", index, expected) == 1;
    }

    /** 检查当前代次尚未确认的任务，重试中的任务同样计入，不能把部分索引宣布为完整。 */
    public long remaining(long epoch) {
        return one("SELECT COUNT(*) n FROM search_sync_task WHERE dirty=1 OR applied_epoch<>?", epoch).getLong("n");
    }

    /** 串行检查代次与完成条件后切换别名；少量维护网络调用在有界事务内，业务写入不持有此状态锁。 */
    @Transactional(timeout = 5)
    public boolean complete(long epoch, Runnable switchAlias) {
        Row row = one("SELECT * FROM search_index_state WHERE id=1 FOR UPDATE");
        if (row.getLong("epoch") != epoch || remaining(epoch) != 0) return false;
        switchAlias.run();
        queries.updateBySql("UPDATE search_index_state SET ready=1,last_error=NULL WHERE id=1 AND epoch=?", epoch);
        return true;
    }

    /** 清理已退休索引的登记；清理失败时仍保留记录，供下一轮重试。 */
    public void retiredCleaned(String name) { queries.updateBySql("UPDATE search_index_state SET retired_index=NULL WHERE id=1 AND retired_index=?", name); }

    /** 同步状态只记录分类错误，不记录索引正文及认证资料。 */
    public void stateFailed() { queries.updateBySql("UPDATE search_index_state SET last_error='SEARCH_ENGINE_UNAVAILABLE' WHERE id=1"); }

    /** 成功轮询后清理当前代次的瞬时故障提示，不掩盖持久任务自身的失败记录。 */
    public void stateSucceeded(long epoch) { queries.updateBySql("UPDATE search_index_state SET last_error=NULL WHERE id=1 AND epoch=?", epoch); }

    /** 固定查询的单行读取，使用当前Spring上下文的Flex会话及事务。 */
    private Row one(String sql, Object... args) {
        var rows = queries.selectListBySql(sql, args); return rows.isEmpty() ? null : rows.get(0);
    }
}
