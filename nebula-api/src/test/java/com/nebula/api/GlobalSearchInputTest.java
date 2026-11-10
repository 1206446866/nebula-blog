package com.nebula.api;

import com.nebula.api.search.*;
import com.nebula.api.service.impl.*;
import com.nebula.auth.security.AuthLoginUser;
import com.nebula.common.exception.BusinessException;
import com.nebula.common.security.ResourceLimitProperties;
import com.nebula.user.entity.User;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
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
}
