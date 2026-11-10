package com.nebula.api.service.impl;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.mybatisflex.core.paginate.Page;
import com.nebula.api.service.GlobalSearchService;
import com.nebula.api.search.*;
import com.nebula.api.vo.search.*;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.exception.code.SearchErrorCode;
import com.nebula.common.security.ResourceLimitProperties;
import com.nebula.common.util.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** ES提供排序候选，本地MySQL核验当前权限、版本、原包含规则和公开展示字段。 */
@Service
public class GlobalSearchServiceImpl implements GlobalSearchService {
    private static final List<String> TYPES = List.of("article", "comment", "user", "category", "tag");
    private final SearchSourceService source;
    private final SearchProperties properties;
    private final ResourceLimitProperties limits;
    private final SearchIndexService index;
    private final ElasticsearchGateway es;
    public GlobalSearchServiceImpl(SearchSourceService source, SearchProperties properties, ResourceLimitProperties limits,
                                   SearchIndexService index, ElasticsearchGateway es) {
        this.source=source;this.properties=properties;this.limits=limits;this.index=index;this.es=es;
    }

    @Override
    @Transactional(readOnly=true, timeoutString="${nebula.resources.search-timeout-seconds:3}")
    public SearchResultVO search(String keyword, String type, int current, int size) {
        if (SecurityUtils.getLoginUser()==null) throw new AccessDeniedException("请先登录");
        keyword=keyword==null?"":keyword.strip();
        if (keyword.length()>50 || !SearchText.validKeyword(keyword) || current<1 || size<1 || size>50
                || (!"all".equals(type) && !TYPES.contains(type))) throw new BusinessException(SearchErrorCode.INVALID_SEARCH);
        var counts=new LinkedHashMap<String,Long>();TYPES.forEach(t->counts.put(t,0L));
        if(keyword.isBlank()) return new SearchResultVO(Page.of(1,size,0),counts);
        if(SearchText.fold(keyword).isBlank()) throw new BusinessException(SearchErrorCode.INVALID_SEARCH);
        if(properties.getBackend().equals("mysql")) return source.search(keyword,type,current,size);
        var allowed=new ArrayList<String>();allowed.add("user");
        if(SecurityUtils.hasPermission("article:list")) {allowed.add("article");allowed.add("comment");}
        if(SecurityUtils.hasPermission("category:list")) allowed.add("category");
        if(SecurityUtils.hasPermission("tag:list")) allowed.add("tag");
        if(!type.equals("all")&&!allowed.contains(type)) return new SearchResultVO(Page.of(1,size,0),counts);
        long deadline=System.nanoTime()+limits.getSearchTimeoutSeconds()*1_000_000_000L;
        String pit=null;long rawTotal=0;boolean exhausted=false;
        int maxWindow=properties.getMaxWindow(),seen=0;
        long needed=Math.min((long)current*size+1,(long)maxWindow+1);
        var verified=new LinkedHashMap<String,SearchItemVO>();
        List<FieldValue> after=null;
        try {
            String alias=index.readyAlias();
            pit=es.openPointInTime(alias,remaining(deadline));
            if(pit.isBlank()) throw ElasticsearchGateway.unavailable();
            while(!exhausted && verified.size()<needed && seen<maxWindow) {
                var filters=new ArrayList<Query>();
                filters.add(Query.of(q -> q.term(t -> t.field("live").value(true))));
                filters.add(Query.of(q -> q.terms(t -> t.field("kind").terms(v -> v.value(allowed.stream().map(FieldValue::of).toList())))));
                String pattern=SearchText.literalPattern(keyword);
                filters.add(Query.of(q -> q.wildcard(w -> w.field("text").value(pattern))));
                String snapshot=pit;
                var request=new SearchRequest.Builder().query(q -> q.bool(b -> b.filter(filters)))
                        .pit(p -> p.id(snapshot).keepAlive(t -> t.time("30s"))).size(Math.min(200,maxWindow-seen))
                        .sort(s -> s.field(f -> f.field("occurred_at").order(SortOrder.Desc)))
                        .sort(s -> s.field(f -> f.field("kind").order(SortOrder.Asc)))
                        .sort(s -> s.field(f -> f.field("id").order(SortOrder.Desc)))
                        .trackTotalHits(t -> t.enabled(true)).version(true)
                        .source(s -> s.filter(f -> f.includes("kind","id")))
                        .timeout(Math.max(1,Math.min(1000,remaining(deadline)))+"ms");
                if(!type.equals("all")) request.postFilter(q -> q.term(t -> t.field("kind").value(type)));
                if(after!=null) request.searchAfter(after);
                if(seen==0) request.aggregations("types",a -> a.terms(t -> t.field("kind").size(5)));
                var response=es.search(request.build(),remaining(deadline));
                if(response.timedOut()||response.shards().failed().intValue()>0)
                    throw ElasticsearchGateway.unavailable();
                if(response.pitId()!=null) pit=response.pitId();
                if(seen==0) {
                    if(response.hits().total()==null || !response.aggregations().containsKey("types")) throw ElasticsearchGateway.unavailable();
                    rawTotal=response.hits().total().value();
                    for(var bucket:response.aggregations().get("types").sterms().buckets().array()) {
                        String kind=bucket.key().stringValue();
                        if(allowed.contains(kind)) counts.put(kind,bucket.docCount());
                    }
                    if((long)(current-1)*size>=maxWindow && rawTotal>maxWindow)
                        throw new BusinessException(SearchErrorCode.SEARCH_WINDOW);
                }
                var hits=new ArrayList<SearchHit>();
                for(var hit:response.hits().hits()) {
                    if(hit.source()==null) throw ElasticsearchGateway.unavailable();
                    String kind=hit.source().kind();
                    long id=hit.source().id();
                    if(!allowed.contains(kind)||id<=0||hit.version()==null||hit.sort().isEmpty()) throw ElasticsearchGateway.unavailable();
                    hits.add(new SearchHit(kind,id,hit.version(),hit.sort()));
                }
                remaining(deadline);
                var live=source.visibleMatches(hits,keyword);
                for(var hit:hits) {
                    var value=live.get(hit.key());
                    if(value!=null) verified.putIfAbsent(hit.key(),value);
                    else counts.computeIfPresent(hit.kind(),(k,v)->Math.max(0,v-1));
                }
                seen+=hits.size();
                exhausted=hits.isEmpty()||seen>=rawTotal;
                if(!hits.isEmpty()) after=hits.get(hits.size()-1).sort();
            }
            remaining(deadline);
            boolean fullyVerified=exhausted&&type.equals("all");
            boolean exact=fullyVerified&&index.synchronizedWithSource();
            if(fullyVerified) {counts.replaceAll((k,v)->0L);for(var item:verified.values())counts.compute(item.type(),(k,v)->v+1);}
            else if(exhausted) counts.put(type,(long)verified.size());
            long total=type.equals("all")?counts.values().stream().mapToLong(Long::longValue).sum():counts.get(type);
            long pageNumber=exhausted?Math.min(current,Math.max(1,(verified.size()+size-1)/size)):current;
            var page=Page.<SearchItemVO>of(pageNumber,size,Math.min(total,maxWindow));
            page.setRecords(verified.values().stream().skip((pageNumber-1)*size).limit(size).toList());
            return new SearchResultVO(page,counts,exact,rawTotal>maxWindow);
        } finally {
            if(pit!=null)try {es.closePointInTime(pit,200);} catch(Exception ignored) { }
        }
    }

    /** ES网络请求共享整条搜索的剩余预算，不能按翻页批次重新计算总时限。 */
    private int remaining(long deadline) {
        long ms=(deadline-System.nanoTime())/1_000_000L;
        if(ms<=0) throw ElasticsearchGateway.unavailable();
        return (int)Math.min(ms,properties.getRequestTimeoutMs());
    }
}
