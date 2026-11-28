package com.nebula.api;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.nebula.api.search.*;
import com.nebula.api.service.impl.*;
import com.nebula.api.vo.search.SearchItemVO;
import com.nebula.auth.security.AuthLoginUser;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.security.ResourceLimitProperties;
import com.nebula.user.entity.User;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 输入保护不得触发SQL、ES或索引状态读取，显式MySQL模式遵守同一规则。 */
class GlobalSearchInputTest {
    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void pureInvisibleKeywordsNeverReachEitherBackend() {
        var principal=new AuthLoginUser(User.create().setId(1L),List.of("SUPER_ADMIN"),List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
        var source=mock(SearchSourceService.class);var index=mock(SearchIndexService.class);var es=mock(ElasticsearchGateway.class);
        var properties=new SearchProperties();
        var search=new GlobalSearchServiceImpl(source,properties,new ResourceLimitProperties(),index,es);
        for(String backend:List.of("mysql","elasticsearch")) {
            properties.setBackend(backend);
            for(String keyword:List.of("\u200d","\ufe0f","\u0301","\u200b \u200d"))
                assertThrows(BusinessException.class,()->search.search(keyword,"all",1,20));
        }
        verifyNoInteractions(source,index,es);
        assertEquals(0,search.search("  ","all",1,20).page().getTotalRow());
        verifyNoInteractions(source,index,es);
    }

    @Test void guestEsCountsOnlyVerifiedRowsEvenWhenIndexHasMoreCandidates() {
        var source = mock(SearchSourceService.class);
        var index = mock(SearchIndexService.class);
        var es = mock(ElasticsearchGateway.class);
        when(index.readyAlias()).thenReturn("nebula-search");
        when(es.openPointInTime(eq("nebula-search"), anyInt())).thenReturn("guest-pit");
        when(es.search(any(), anyInt())).thenReturn(SearchResponse.of(response -> response
                .took(1).timedOut(false).shards(shards -> shards.total(1).successful(1).failed(0))
                .hits(hits -> hits.total(total -> total.value(800).relation(co.elastic.clients.elasticsearch.core.search.TotalHitsRelation.Eq))
                        .hits(List.of(candidate("article", 4), candidate("article", 3), candidate("article", 2), candidate("tag", 1))))));
        when(source.visibleMatches(anyList(), eq("公开"))).thenReturn(Map.of(
                "article:4", new SearchItemVO("article", 4L, 4L, "公开", null, null, null),
                "article:2", new SearchItemVO("article", 2L, 2L, "公开", null, null, null),
                "tag:1", new SearchItemVO("tag", 1L, 1L, "公开", null, null, null)));
        var result = new GlobalSearchServiceImpl(source, new SearchProperties(), new ResourceLimitProperties(), index, es)
                .search("公开", "article", 1, 1);
        assertEquals(2, result.page().getTotalRow());
        assertEquals(List.of(4L), result.page().getRecords().stream().map(SearchItemVO::id).toList());
        assertEquals(Map.of("article", 2L, "comment", 0L, "user", 0L, "category", 0L, "tag", 1L), result.counts());
        assertFalse(result.countsExact());
        assertFalse(result.windowLimited());
        var request = org.mockito.ArgumentCaptor.forClass(SearchRequest.class);
        verify(es).search(request.capture(), anyInt());
        assertNull(request.getValue().postFilter());
        assertTrue(request.getValue().aggregations().isEmpty());
        verify(es).closePointInTime("guest-pit", 200);
    }

    private Hit<ElasticsearchGateway.Candidate> candidate(String kind, long id) {
        return Hit.of(hit -> hit.index("nebula-search").id(kind + ":" + id).version(1L)
                .sort(FieldValue.of(id)).source(new ElasticsearchGateway.Candidate(kind, id)));
    }
}
