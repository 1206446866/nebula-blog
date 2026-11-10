package com.nebula.api.search;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientOptions;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.common.exception.ResourceLimitException;
import jakarta.annotation.PreDestroy;
import org.apache.http.HttpHost;
import org.apache.http.auth.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.elasticsearch.client.*;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/** 官方ES客户端入口：管理连接生命周期、索引范围和可取消的请求预算，错误不暴露原始正文与凭据。 */
@Component
@lombok.extern.slf4j.Slf4j
public class ElasticsearchGateway {
    private final SearchProperties properties;
    private final ObjectMapper json;
    private Connection connection;

    /** 类型化候选只保留身份，公开展示字段由MySQL重新读取。 */
    public record Candidate(String kind, long id) {}

    public ElasticsearchGateway(SearchProperties properties, ObjectMapper json) {
        this.properties = properties;
        this.json = json;
        validateEndpoint(properties.getEndpoint());
    }

    /** 区分索引不存在和引擎故障，认证或网络错误不能被当成404。 */
    public boolean exists(String index) {
        index(index);
        return execute(properties.getRequestTimeoutMs(), c -> c.indices().exists(r -> r.index(index))).value();
    }

    /** 建立公开候选索引，保留字面包含匹配与字段边界。 */
    public void create(String index) {
        index(index);
        var response = execute(properties.getRequestTimeoutMs(), c -> c.indices().create(r -> r.index(index)
                .settings(s -> s.numberOfShards("1").numberOfReplicas("0").refreshInterval(t -> t.time("1s")))
                .mappings(m -> m.dynamic(DynamicMapping.Strict)
                        .properties("kind", p -> p.keyword(k -> k))
                        .properties("id", p -> p.long_(n -> n))
                        .properties("occurred_at", p -> p.long_(n -> n))
                        .properties("live", p -> p.boolean_(b -> b))
                        .properties("text", p -> p.wildcard(w -> w)))));
        if (!response.acknowledged()) throw unavailable();
    }

    /** 逐项确认批量写入；旧版本冲突幂等完成，其他失败保留给持久队列重试。 */
    public List<Boolean> bulk(String index, List<SearchSyncStore.Document> documents) {
        index(index);
        var operations = new ArrayList<BulkOperation>();
        for (var document : documents) {
            operations.add(BulkOperation.of(b -> b.index(i -> i.index(index).id(document.key())
                    .version(document.generation()).versionType(VersionType.ExternalGte).document(document.body()))));
        }
        var response = execute(properties.getRequestTimeoutMs(), c -> c.bulk(r -> r.operations(operations).refresh(Refresh.WaitFor)));
        if (response.items().size() != documents.size()) throw unavailable();
        return response.items().stream().map(item -> item.status() >= 200 && item.status() < 300 || item.status() == 409).toList();
    }

    /** 开启有界快照，保持同一次查询的候选排序稳定。 */
    public String openPointInTime(String alias, int timeoutMs) {
        index(alias);
        return execute(timeoutMs, c -> c.openPointInTime(r -> r.index(alias).keepAlive(t -> t.time("30s")))).id();
    }

    /** 类型化查询与响应；部分分片或超时结果由业务层拒绝，不能当完整命中。 */
    public SearchResponse<Candidate> search(SearchRequest request, int timeoutMs) {
        return execute(timeoutMs, c -> c.search(request, Candidate.class));
    }

    /** 在独立短预算内释放PIT，请求失败不会覆盖原搜索结果或异常。 */
    public void closePointInTime(String id, int timeoutMs) {
        execute(timeoutMs, c -> c.closePointInTime(r -> r.id(id)));
    }

    /** 只删除已验证属于本应用的物理索引。 */
    public void delete(String index) {
        index(index);
        if (!execute(properties.getRequestTimeoutMs(), c -> c.indices().delete(r -> r.index(index))).acknowledged())
            throw unavailable();
    }

    /** 同一预算内原子切换本应用别名，重复切换同一目标同样安全。 */
    public void switchAlias(String target) {
        index(target);
        String alias = properties.getAlias();
        long deadline = System.nanoTime() + properties.getRequestTimeoutMs() * 1_000_000L;
        var actions = new ArrayList<Action>();
        if (execute(remaining(deadline), c -> c.indices().exists(r -> r.index(alias))).value()) {
            var aliases = execute(remaining(deadline), c -> c.indices().getAlias(r -> r.name(alias)));
            for (String previous : aliases.result().keySet()) {
                index(previous);
                actions.add(Action.of(a -> a.remove(r -> r.index(previous).alias(alias))));
            }
        }
        actions.add(Action.of(a -> a.add(r -> r.index(target).alias(alias))));
        if (!execute(remaining(deadline), c -> c.indices().updateAliases(r -> r.actions(actions))).acknowledged())
            throw unavailable();
    }

    /** 可变索引名不能越过当前应用的命名空间。 */
    public void index(String value) {
        if (!value.equals(properties.getAlias()) && !value.matches(java.util.regex.Pattern.quote(properties.getAlias()) + "-v1-[a-f0-9]{32}"))
            throw new IllegalArgumentException("索引不属于当前应用");
    }

    /** 为每次调用设置连接池等待、连接和读取预算，超时或线程中断时取消底层HTTP请求。 */
    private <T> T execute(int requestedMs, Function<ElasticsearchAsyncClient, CompletableFuture<T>> operation) {
        int budget = Math.max(1, Math.min(requestedMs, properties.getRequestTimeoutMs()));
        long deadline = System.nanoTime() + budget * 1_000_000L;
        CompletableFuture<T> future = null;
        try {
            var config = RequestConfig.custom().setConnectionRequestTimeout(budget)
                    .setConnectTimeout(Math.min(1000, budget)).setSocketTimeout(budget).build();
            var options = RequestOptions.DEFAULT.toBuilder().setRequestConfig(config)
                    .setHttpAsyncResponseConsumerFactory(new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(16 * 1024 * 1024)).build();
            future = operation.apply(client().withTransportOptions(new RestClientOptions(options, false)));
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) throw new TimeoutException();
            return future.get(remaining, java.util.concurrent.TimeUnit.NANOSECONDS);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (Exception failure) {
            Throwable cause = failure instanceof ExecutionException ? failure.getCause() : failure;
            if (cause instanceof ElasticsearchException error) {
                String category = error.error().type();
                log.warn("搜索引擎请求未成功，状态：{}，错误分类：{}", error.status(),
                        category != null && category.matches("[a-z_]{1,80}") ? category : "unknown");
            }
            throw unavailable();
        } finally {
            if (future != null && !future.isDone()) future.cancel(true);
        }
    }

    /** 复用一个连接池；配置变化时替换并关闭旧连接，MySQL模式允许尚未配置ES凭据。 */
    private synchronized ElasticsearchAsyncClient client() {
        if (properties.getPassword().isBlank()) throw unavailable();
        if (connection != null && connection.matches(properties)) return connection.client;
        close();
        URI endpoint = validateEndpoint(properties.getEndpoint());
        var credentials = new BasicCredentialsProvider();
        int port = endpoint.getPort() < 0 ? (endpoint.getScheme().equals("https") ? 443 : 80) : endpoint.getPort();
        credentials.setCredentials(new AuthScope(endpoint.getHost(), port),
                new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword()));
        var rest = RestClient.builder(new HttpHost(endpoint.getHost(), port, endpoint.getScheme()))
                .setHttpClientConfigCallback(builder -> builder.setDefaultCredentialsProvider(credentials)
                        .setDefaultIOReactorConfig(IOReactorConfig.custom().setIoThreadCount(2).build())
                        .setMaxConnTotal(32).setMaxConnPerRoute(32)
                        .setRoutePlanner(new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE))
                        .setRedirectStrategy(new DefaultRedirectStrategy() {
                            @Override public boolean isRedirected(org.apache.http.HttpRequest request,
                                    org.apache.http.HttpResponse response, org.apache.http.protocol.HttpContext context) { return false; }
                        })).build();
        var transport = new RestClientTransport(rest, new JacksonJsonpMapper(json));
        connection = new Connection(properties, transport, new ElasticsearchAsyncClient(transport));
        return connection.client;
    }

    /** 限定本机HTTP或远程HTTPS，不允许URL携带凭据、查询参数和额外路径。 */
    private static URI validateEndpoint(String value) {
        URI endpoint = URI.create(value);
        if (endpoint.getUserInfo() != null || endpoint.getQuery() != null || endpoint.getFragment() != null
                || !List.of("http", "https").contains(endpoint.getScheme()) || endpoint.getHost() == null
                || !List.of("", "/").contains(endpoint.getPath())) throw new IllegalArgumentException("ES地址须为不含凭据和路径的HTTP(S)地址");
        if (endpoint.getScheme().equals("http") && !List.of("localhost", "127.0.0.1", "[::1]", "::1").contains(endpoint.getHost()))
            throw new IllegalArgumentException("远程ES连接须使用HTTPS");
        return endpoint;
    }

    /** 维护多次网络调用共享的剩余时限。 */
    private int remaining(long deadline) {
        long value = (deadline - System.nanoTime()) / 1_000_000;
        if (value <= 0) throw unavailable();
        return (int) Math.min(value, properties.getRequestTimeoutMs());
    }

    /** 应用关闭或连接配置替换时释放HTTP线程与连接，不输出认证资料。 */
    @PreDestroy
    public synchronized void close() {
        if (connection != null) {
            try { connection.transport.close(); } catch (Exception ignored) { log.warn("搜索连接关闭未完成"); }
            connection = null;
        }
    }

    /** 凭据只用于判断连接是否仍适用，禁止生成包含凭据的描述信息。 */
    private static final class Connection {
        private final String endpoint, username, password;
        private final RestClientTransport transport;
        private final ElasticsearchAsyncClient client;
        private Connection(SearchProperties properties, RestClientTransport transport, ElasticsearchAsyncClient client) {
            endpoint = properties.getEndpoint(); username = properties.getUsername(); password = properties.getPassword();
            this.transport = transport; this.client = client;
        }
        private boolean matches(SearchProperties properties) {
            return endpoint.equals(properties.getEndpoint()) && username.equals(properties.getUsername()) && password.equals(properties.getPassword());
        }
    }

    /** 搜索专用的503保护响应，业务保存不依赖ES成功。 */
    public static ResourceLimitException unavailable() {
        return new ResourceLimitException(503, 2, "搜索服务暂时不可用或索引准备中，请稍后重试");
    }
}
