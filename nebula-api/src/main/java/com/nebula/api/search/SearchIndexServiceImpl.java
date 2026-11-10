package com.nebula.api.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class SearchIndexServiceImpl implements SearchIndexService {
    private final SearchSyncStore store;
    private final ElasticsearchGateway es;
    private final SearchProperties properties;
    private final ObjectMapper json;
    private final ReentrantLock running = new ReentrantLock();
    private volatile long retryAfter;
    public SearchIndexServiceImpl(SearchSyncStore store, ElasticsearchGateway es, SearchProperties properties, ObjectMapper json) {
        this.store = store; this.es = es; this.properties = properties; this.json = json;
    }

    @Override
    @Scheduled(scheduler = "nebulaSearchScheduler", fixedDelayString = "${nebula.search.poll-delay-ms:1000}", initialDelayString = "${nebula.search.initial-delay-ms:2000}")
    public void synchronize() {
        if (!properties.getBackend().equals("elasticsearch") || !properties.isSynchronizationEnabled()
                || System.currentTimeMillis() < retryAfter || !running.tryLock()) return;
        var documents = new ArrayList<SearchSyncStore.Document>();
        try {
            var state = store.state();
            if (state == null) throw new IllegalStateException("搜索迁移尚未执行");
            String index = state.getString("index_name");
            if (index == null || state.getInt("rebuild_requested") == 1 || !es.exists(index)) {
                String next = properties.getAlias() + "-v1-" + UUID.randomUUID().toString().replace("-", "");
                es.create(next);
                if (!store.install(state.getLong("epoch"), next)) { es.delete(next); return; }
                state = store.state(); index = state.getString("index_name");
            }
            long epoch = state.getLong("epoch"); int bytes = 0;
            for (var task : store.pending(epoch, state.getInt("ready") == 1, properties.getBatchSize())) {
                var document = store.snapshot(task);
                int size = json.writeValueAsBytes(document.body()).length + 256;
                if (!documents.isEmpty() && bytes + size > 4 * 1024 * 1024) break;
                if (size > 8 * 1024 * 1024) { store.failed(document, "DOCUMENT_TOO_LARGE"); continue; }
                documents.add(document); bytes += size;
            }
            if (!documents.isEmpty()) {
                var outcomes = es.bulk(index, documents);
                for (int i = 0; i < documents.size(); i++) {
                    if (outcomes.get(i)) store.acknowledge(documents.get(i), epoch);
                    else store.failed(documents.get(i), "INDEX_ITEM_REJECTED");
                }
            }
            if (state.getInt("ready") == 0 && store.remaining(epoch) == 0) {
                String target = index;
                store.complete(epoch, () -> switchAlias(target));
            }
            String retired = state.getString("retired_index");
            if (retired != null && store.state().getInt("ready") == 1) {
                es.index(retired);
                if (es.exists(retired)) es.delete(retired);
                store.retiredCleaned(retired);
            }
            store.stateSucceeded(epoch);
        } catch (Exception failure) {
            for (var doc : documents) try { store.failed(doc, "SEARCH_ENGINE_UNAVAILABLE"); } catch (Exception ignored) { }
            try { store.stateFailed(); } catch (Exception ignored) { }
            retryAfter = System.currentTimeMillis() + 5000;
            log.warn("搜索索引同步暂不可用，持久任务将在稍后重试；错误分类：{}", failure.getClass().getSimpleName());
        } finally { running.unlock(); }
    }

    @Override public Map<String, Object> status() {
        var state = store.state(); var result = new LinkedHashMap<String, Object>();
        result.put("backend", properties.getBackend()); result.put("ready", state != null && state.getInt("ready") == 1);
        if (state != null) {
            result.put("pending", store.remaining(state.getLong("epoch"))); result.put("rebuildRequested", state.getInt("rebuild_requested") == 1);
            result.put("lastError", state.getString("last_error")); result.put("epoch", state.getLong("epoch"));
            var failure = store.failures(state.getLong("epoch"));
            result.put("failedTasks", failure.getLong("failed_tasks")); result.put("taskError", failure.getString("task_error"));
        }
        return result;
    }
    @Override public void requestRebuild() { store.requestRebuild(); }
    @Override public boolean synchronizedWithSource() { return !store.hasPending(); }
    @Override public String readyAlias() {
        try {
            var state = store.state();
            if (state == null || state.getInt("ready") != 1) throw ElasticsearchGateway.unavailable();
            return properties.getAlias();
        } catch (org.springframework.dao.DataAccessException failure) { throw ElasticsearchGateway.unavailable(); }
    }

    /** 只移除当前应用别名的绑定；完整代次构建成功后原子切换，重试切换同样安全。 */
    private void switchAlias(String index) {
        es.switchAlias(index);
    }
}
