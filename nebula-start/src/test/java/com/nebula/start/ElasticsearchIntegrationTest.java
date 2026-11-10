package com.nebula.start;

import com.fasterxml.jackson.databind.*;
import com.mybatisflex.core.row.Db;
import com.nebula.api.search.*;
import com.nebula.api.service.GlobalSearchService;
import com.nebula.api.vo.search.SearchResultVO;
import com.nebula.article.entity.Article;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.auth.security.AuthLoginUser;
import com.nebula.auth.util.JwtUtil;
import com.nebula.category.entity.Category;
import com.nebula.category.mapper.CategoryMapper;
import com.nebula.comment.entity.Comment;
import com.nebula.comment.mapper.CommentMapper;
import com.nebula.tag.entity.Tag;
import com.nebula.tag.mapper.TagMapper;
import com.nebula.user.entity.*;
import com.nebula.user.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 真实MySQL与Docker ES回归：固定隔离库、专用索引、测试Redis分库，不能写入开发业务数据。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/nebula_es_20261107?serverTimezone=Asia/Shanghai",
        "spring.data.redis.database=14", "nebula.publication.enabled=false",
        "nebula.search.alias=nebula-search-it", "nebula.search.initial-delay-ms=3600000",
        "nebula.search.request-timeout-ms=3000", "nebula.resources.key-prefix=nebula:es-integration:",
        "nebula.resources.search.account=2000", "nebula.resources.search.ip=4000"
})
@AutoConfigureMockMvc
class ElasticsearchIntegrationTest extends JwtIntegrationTestSupport {
    @Autowired ArticleMapper articles;
    @Autowired CommentMapper comments;
    @Autowired UserMapper users;
    @Autowired UserRoleMapper roles;
    @Autowired CategoryMapper categories;
    @Autowired TagMapper tags;
    @Autowired GlobalSearchService search;
    @Autowired SearchIndexService index;
    @Autowired SearchSyncStore store;
    @Autowired ElasticsearchGateway es;
    @Autowired SearchProperties settings;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtUtil jwt;
    final List<Long> articleIds=new ArrayList<>(),commentIds=new ArrayList<>(),userIds=new ArrayList<>(),categoryIds=new ArrayList<>(),tagIds=new ArrayList<>();
    final List<String> tokens=new ArrayList<>();
    User owner;
    String marker;

    @BeforeEach void setup() {
        assertEquals("nebula_es_20261107",Db.selectOneBySql("SELECT DATABASE() name").getString("name"));
        marker="esit"+UUID.randomUUID().toString().replace("-", "").substring(0,12);
        owner=user(1L);login(owner,List.of("SUPER_ADMIN"),List.of());
    }
    private User user(long role) {
        var user=User.create().setNid(marker+"_"+userIds.size()).setUsername("测试作者").setPassword("unused")
                .setEmail(UUID.randomUUID()+"@example.invalid").setStatus(0).setDeleted(0);
        users.insert(user);userIds.add(user.getId());roles.insert(UserRole.create().setUserId(user.getId()).setRoleId(role));return user;
    }
    private Article article(String content,int status) {
        var a=Article.create().setTitle("检索样本").setContent(content).setUserId(owner.getId()).setAuthor(owner.getUsername())
                .setStatus(status).setDeleted(0).setViewCount(0L).setLikeCount(0);
        articles.insert(a);articleIds.add(a.getId());return a;
    }
    private Comment comment(Article a,String content,int status) {
        var c=Comment.create().setArticleId(a.getId()).setUserId(owner.getId()).setParentId(0L).setContent(content).setStatus(status).setLikeCount(0L);
        comments.insert(c);commentIds.add(c.getId());return c;
    }
    private void login(User user,List<String> role,List<String> permissions) {
        var principal=new AuthLoginUser(user,role,permissions);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
    }
    private String token() {var token=jwt.createToken(owner);tokens.add(token);return "Bearer "+token;}
    private void synchronizedIndex() throws Exception {
        long until=System.nanoTime()+90_000_000_000L;
        while(System.nanoTime()<until) {
            index.synchronize();var status=index.status();
            if(Boolean.TRUE.equals(status.get("ready"))&&((Number)status.get("pending")).longValue()==0)return;
            Thread.sleep(150);
        }
        fail("索引未在预算内追平："+index.status());
    }
    private List<Long> ids(SearchResultVO response) {return response.page().getRecords().stream().map(v->v.id()).toList();}

    @AfterEach void cleanup() throws Exception {
        SecurityContextHolder.clearContext();
        for(String token:tokens)jwt.revokeToken(token);
        for(long id:commentIds)Db.deleteBySql("DELETE FROM comment WHERE id=? AND user_id=?",id,owner.getId());
        for(long id:articleIds)Db.deleteBySql("DELETE FROM article WHERE id=? AND user_id=?",id,owner.getId());
        for(long id:categoryIds)Db.deleteBySql("DELETE FROM category WHERE id=? AND name LIKE ?",id,marker+"%");
        for(long id:tagIds)Db.deleteBySql("DELETE FROM tag WHERE id=? AND name LIKE ?",id,marker+"%");
        for(long id:userIds){Db.deleteBySql("DELETE FROM user_role WHERE user_id=?",id);Db.deleteBySql("DELETE FROM user WHERE id=? AND nid LIKE ?",id,marker+"%");}
        synchronizedIndex();
    }

    /** 通过独立HTTP核对引擎实际状态，避免用被测SDK解析器生成自身预期。 */
    private JsonNode raw(String method,String path,Object body)throws Exception {
        String auth=Base64.getEncoder().encodeToString((settings.getUsername()+":"+settings.getPassword()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(settings.getEndpoint()+path))
                .timeout(java.time.Duration.ofSeconds(3)).header("Authorization","Basic "+auth).header("Content-Type","application/json")
                .method(method,body==null?java.net.http.HttpRequest.BodyPublishers.noBody():java.net.http.HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response=java.net.http.HttpClient.newHttpClient().send(request.build(),java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode(),"独立ES状态读取失败");
        return json.readTree(response.body());
    }

    @Test void httpSearchCoversFiveTypesAndNeverReturnsDraftOrPrivateFields() throws Exception {
        owner.setUsername(marker);users.update(owner);var a=article(marker,1);article(marker,0);comment(a,marker,0);comment(a,marker,1);
        var category=Category.create().setName(marker+"分类").setDescription(marker).setDeleted(0);categories.insert(category);categoryIds.add(category.getId());
        var tag=Tag.create().setName(marker+"标签").setDescription(marker).setDeleted(0);tags.insert(tag);tagIds.add(tag.getId());
        synchronizedIndex();
        mvc.perform(get("/search").header("Authorization",token()).param("q",marker))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.counts.article").value(1))
                .andExpect(jsonPath("$.data.counts.comment").value(1)).andExpect(jsonPath("$.data.counts.user").value(1))
                .andExpect(jsonPath("$.data.counts.category").value(1)).andExpect(jsonPath("$.data.counts.tag").value(1))
                .andExpect(jsonPath("$.data.page.totalRow").value(5)).andExpect(jsonPath("$.data.countsExact").value(true))
                .andExpect(jsonPath("$.data.page.records[*].password").isEmpty()).andExpect(jsonPath("$.data.page.records[*].email").isEmpty());
        var draft=raw("GET","/"+store.state().getString("index_name")+"/_doc/article:"+articleIds.get(1),null);
        assertFalse(draft.path("_source").path("live").asBoolean());assertEquals(0,draft.path("_source").path("text").size());
    }

    @Test void realMysqlCollationIsTheIndependentOracleForLiteralUnicodeAndSymbolCases() throws Exception {
        var contents=List.of("星云博客","星空云海","的地得","a i de in is the","data database","Java spring boot","Sarah","aaaa",
                "C++ C#","a,b","a.b","ab cd","a bc","ab","bc","abc","literal%_!","\"quoted\" +required -excluded",
                "&& (java) a/b a\\b foo@bar x:y","😀🚀星云","👨‍👩‍👧 family","café cafe\u0301 CAFÉ","straße STRASSE","ＡＢＣ abc123",
                "零宽\u200b字符","naïve résumé","中文English混排","αβ σςΣ");
        contents.forEach(v->article(v,1));synchronizedIndex();
        var queries=new ArrayList<>(List.of("星","星云","云博","的","a","i","de","in","is","the","data","java","Sarah","aa","C++","C#",
                "a,b","a.b","ab cd","a bc","ab","bc","abc","%","_","!","%_!","\"","+","-","&&","(java)","a/b","a\\b","foo@bar","x:y",
                "😀","🚀","👨‍👩‍👧","👨‍","‍👩","\u200d","café","cafe","cafe\u0301","STRASSE","straße","ＡＢＣ","abc123","零宽\u200b字符","é","e","σ","ς","*","?","' OR 1=1 --"));
        var rng=new Random(610311);for(int i=0;i<20;i++){String text=contents.get(rng.nextInt(contents.size()));int at=rng.nextInt(text.length());if(Character.isLowSurrogate(text.charAt(at)))at--;queries.add(text.substring(at,Math.min(text.length(),at+Math.min(3,text.length()-at))));}
        var checks=new ArrayList<org.junit.jupiter.api.function.Executable>();
        for(String q:queries) {
            if(!q.isEmpty()&&Character.isHighSurrogate(q.charAt(q.length()-1)))continue;
            checks.add(()->{
            // 入口原有约定会去除首尾空白；参考查询仍由独立的原SQL计算，不能拿新搜索结果当预期。
            String normalized=q.strip();
            // 纯不可见关键词现明确拒绝，不能通过隐式MySQL回退执行全表包含扫描。
            if(!normalized.isBlank() && SearchText.fold(normalized).isBlank()) {
                var invalid=assertThrows(com.nebula.common.exception.BusinessException.class,()->search.search(q,"article",1,50));
                assertEquals(1901,invalid.getCode());return;
            }
            String pattern="%"+normalized.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
            List<Long> expected=normalized.isBlank()?List.of():Db.selectListBySql("SELECT id FROM article WHERE deleted=0 AND status=1 AND (title LIKE ? ESCAPE '!' OR content LIKE ? ESCAPE '!' OR author LIKE ? ESCAPE '!') ORDER BY create_time DESC,id DESC",pattern,pattern,pattern)
                    .stream().map(v->v.getLong("id")).toList();
            var actual=search.search(q,"article",1,50);
            assertEquals(expected,ids(actual),"MySQL包含语义不一致，关键词："+q);assertEquals(expected.size(),actual.page().getTotalRow(),q);
            });
        }
        assertAll("逐项对照既有MySQL包含语义",checks);
    }

    @Test void nulIsRejectedAndEmptySearchDoesNotExposeTheWholeIndex() throws Exception {
        article(marker,1);synchronizedIndex();String token=token();
        mvc.perform(get("/search").header("Authorization",token).param("q","\u0000")).andExpect(status().isBadRequest());
        mvc.perform(get("/search").header("Authorization",token).param("q","x".repeat(51))).andExpect(status().isBadRequest());
        mvc.perform(get("/search").header("Authorization",token).param("q"," ")).andExpect(jsonPath("$.data.page.totalRow").value(0));
        mvc.perform(get("/search").param("q",marker)).andExpect(status().isUnauthorized());
    }

    @Test void unpublishAndDeletionAreFilteredBeforeTheIndexHasCaughtUpAndPagesAreFilled() throws Exception {
        for(int i=0;i<30;i++)article(marker,1);synchronizedIndex();
        for(int i=23;i<30;i++)Db.updateBySql("UPDATE article SET status=0 WHERE id=?",articleIds.get(i));
        List<Long> expected=Db.selectListBySql("SELECT id FROM article WHERE status=1 AND user_id=? ORDER BY create_time DESC,id DESC",owner.getId()).stream().map(v->v.getLong("id")).toList();
        var first=search.search(marker,"article",1,12);
        var last=search.search(marker,"article",99,12);
        assertEquals(expected.subList(0,12),ids(first));assertEquals(expected.subList(12,23),ids(last));assertEquals(2,last.page().getPageNumber());assertEquals(23,last.page().getTotalRow());
        Db.deleteBySql("DELETE FROM article WHERE id=? AND user_id=?",expected.get(0),owner.getId());
        assertFalse(ids(search.search(marker,"article",1,50)).contains(expected.get(0)));
    }

    @Test void indexSnapshotCannotReturnWithdrawnContentAndLiveChecksRemoveStaleVersions() throws Exception {
        var a=article(marker,1);synchronizedIndex();String pit=raw("POST","/"+settings.getAlias()+"/_pit?keep_alive=30s",null).path("id").asText();
        try{
            Db.updateBySql("UPDATE article SET status=0 WHERE id=?",a.getId());
            var raw=raw("POST","/_search",Map.of("pit",Map.of("id",pit,"keep_alive","30s"),"query",Map.of("ids",Map.of("values",List.of("article:"+a.getId())))));
            assertEquals(1,raw.path("hits").path("total").path("value").asLong());
            assertTrue(search.search(marker,"article",1,12).page().getRecords().isEmpty());
        }finally{raw("DELETE","/_pit",Map.of("id",pit));}
    }

    @Test void businessRollbackAlsoRollsBackItsDurableSearchTask() throws Exception {
        long[] rolled={0};new TransactionTemplate(transactions).execute(status->{var a=article(marker,1);rolled[0]=a.getId();status.setRollbackOnly();return null;});
        assertEquals(0L,Db.selectOneBySql("SELECT COUNT(*) n FROM search_sync_task WHERE kind='article' AND target_id=?",rolled[0]).getLong("n"));
        synchronizedIndex();assertEquals(0,search.search(marker,"article",1,12).page().getTotalRow());
    }

    @Test void sourceCommitSurvivesSearchOutageAndTheQueuedUpdateRecovers() throws Exception {
        var a=article(marker,1);synchronizedIndex();String endpoint=settings.getEndpoint();
        try{
            settings.setEndpoint("http://127.0.0.1:1");Db.updateBySql("UPDATE article SET content=? WHERE id=?",marker+"fresh",a.getId());index.synchronize();
            assertEquals(marker+"fresh",Db.selectOneBySql("SELECT content FROM article WHERE id=?",a.getId()).getString("content"));
            assertTrue(((Number)index.status().get("pending")).longValue()>0);
            mvc.perform(get("/search").header("Authorization",token()).param("q",marker)).andExpect(status().isServiceUnavailable())
                    .andExpect(header().exists("Retry-After")).andExpect(header().string("Cache-Control","no-store"));
        }finally{settings.setEndpoint(endpoint);}
        synchronizedIndex();login(owner,List.of("SUPER_ADMIN"),List.of());assertEquals(List.of(a.getId()),ids(search.search(marker+"fresh","article",1,12)));
    }

    @Test void repeatedAndOlderEventsCannotResurrectReplacedOrDeletedContent() throws Exception {
        var a=article(marker,1);synchronizedIndex();var old=store.snapshot(new SearchSyncStore.Task("article",a.getId()));
        Db.updateBySql("UPDATE article SET content=? WHERE id=?",marker+"new",a.getId());synchronizedIndex();
        String physical=store.state().getString("index_name");es.bulk(physical,List.of(old));store.acknowledge(old,store.state().getLong("epoch"));
        assertEquals(List.of(a.getId()),ids(search.search(marker+"new","article",1,12)));
        Db.deleteBySql("DELETE FROM article WHERE id=? AND user_id=?",a.getId(),owner.getId());synchronizedIndex();es.bulk(physical,List.of(old));
        var tombstone=raw("GET","/"+physical+"/_doc/article:"+a.getId(),null);
        assertFalse(tombstone.path("_source").path("live").asBoolean());assertEquals(0,tombstone.path("_source").path("text").size());
    }

    @Test void articleVisibilityFansOutAcrossMoreThanOneCommentBatch() throws Exception {
        var a=article("父文章",1);for(int i=0;i<205;i++)comment(a,marker,0);synchronizedIndex();
        Db.updateBySql("UPDATE article SET status=0 WHERE id=?",a.getId());assertEquals(0,search.search(marker,"comment",1,12).page().getTotalRow());synchronizedIndex();
        Db.updateBySql("UPDATE article SET status=1 WHERE id=?",a.getId());synchronizedIndex();
        var page=search.search(marker,"comment",17,12);assertEquals(205,page.page().getTotalRow());assertEquals(12,page.page().getRecords().size());
    }

    @Test void byteLevelTextChangesAreNotLostByCaseInsensitiveTriggerComparisons() throws Exception {
        var a=article("straße",1);synchronizedIndex();long before=Db.selectOneBySql("SELECT generation FROM search_sync_task WHERE kind='article' AND target_id=?",a.getId()).getLong("generation");
        Db.updateBySql("UPDATE article SET content='STRASSE' WHERE id=?",a.getId());
        assertEquals(before+1,Db.selectOneBySql("SELECT generation FROM search_sync_task WHERE kind='article' AND target_id=?",a.getId()).getLong("generation"));
        synchronizedIndex();assertEquals(List.of(a.getId()),ids(search.search("strasse","article",1,12)));
    }

    @Test void roleFilteringAndIndexAdministrationAreCheckedAtTheCurrentRequest() throws Exception {
        article(marker,1);synchronizedIndex();User restricted=user(3L);Db.deleteBySql("DELETE FROM user_role WHERE user_id=?",restricted.getId());
        login(restricted,List.of("USER"),List.of());assertEquals(0,search.search(marker,"article",1,12).page().getTotalRow());
        String token=jwt.createToken(restricted);tokens.add(token);
        mvc.perform(get("/search/index/status").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        mvc.perform(post("/search/index/rebuild").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
    }

    @Test void rebuildingUsesANewIndexAndLeavesBusinessRowsIntact() throws Exception {
        article(marker,1);article(marker,1);synchronizedIndex();String previous=store.state().getString("index_name");
        int batch=settings.getBatchSize();try{settings.setBatchSize(1);index.requestRebuild();index.synchronize();
            assertNotEquals(previous,store.state().getString("index_name"));assertEquals(0,store.state().getInt("ready"));
            assertThrows(com.nebula.common.exception.ResourceLimitException.class,()->search.search(marker,"all",1,12));
        }finally{settings.setBatchSize(batch);}
        synchronizedIndex();assertEquals(2,search.search(marker,"article",1,12).page().getTotalRow());
        assertTrue(es.exists(previous));assertTrue(raw("GET","/_alias/"+settings.getAlias(),null).has(store.state().getString("index_name")));
    }

    @Test void largeCandidateSetsExposeEstimatedCountsAndEnforceTheResultWindow() throws Exception {
        for(int i=0;i<1001;i++)article(marker,1);synchronizedIndex();
        assertFalse(search.search(marker,"article",1,12).countsExact());int window=settings.getMaxWindow();
        try{settings.setMaxWindow(1000);
            mvc.perform(get("/search").header("Authorization",token()).param("q",marker).param("type","article").param("current","1001").param("size","1"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1902));
        }finally{settings.setMaxWindow(window);}
    }

    @Test void authenticationIsRequiredAndTheApplicationCannotManageClusterSecurity() throws Exception {
        var http=java.net.http.HttpClient.newHttpClient();
        var anonymous=http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(settings.getEndpoint()+"/")).GET().build(),java.net.http.HttpResponse.BodyHandlers.discarding());
        assertEquals(401,anonymous.statusCode());
        String credential=Base64.getEncoder().encodeToString((settings.getUsername()+":"+settings.getPassword()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var privileged=http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(settings.getEndpoint()+"/_security/user")).header("Authorization","Basic "+credential).GET().build(),java.net.http.HttpResponse.BodyHandlers.discarding());
        assertEquals(403,privileged.statusCode());
    }

    @Test void fieldsCannotBeJoinedIntoAFalseSubstringAndLongDocumentTailsRemainSearchable() throws Exception {
        var a=article("rightborder",1);Db.updateBySql("UPDATE article SET title='leftborder' WHERE id=?",a.getId());
        var longArticle=article("正文边界".repeat(40000)+marker,1);synchronizedIndex();
        assertEquals(0,search.search("borderright","article",1,12).page().getTotalRow());
        assertEquals(List.of(longArticle.getId()),ids(search.search(marker,"article",1,12)));
    }

    @Test void staleAcknowledgementCannotEraseANewerPendingGeneration() throws Exception {
        var a=article(marker,1);synchronizedIndex();var old=store.snapshot(new SearchSyncStore.Task("article",a.getId()));
        Db.updateBySql("UPDATE article SET content=? WHERE id=?",marker+"fresh",a.getId());store.acknowledge(old,store.state().getLong("epoch"));
        assertEquals(1,Db.selectOneBySql("SELECT dirty FROM search_sync_task WHERE kind='article' AND target_id=?",a.getId()).getInt("dirty"));
        assertFalse(search.search(marker+"fresh","all",1,12).countsExact());
        synchronizedIndex();assertEquals(List.of(a.getId()),ids(search.search(marker+"fresh","article",1,12)));
    }

    @Test void concurrentTextWritesAreCapturedWhileViewCountersDoNotChurnTheIndex() throws Exception {
        var a=article(marker,1);synchronizedIndex();long before=Db.selectOneBySql("SELECT generation FROM search_sync_task WHERE kind='article' AND target_id=?",a.getId()).getLong("generation");
        Db.updateBySql("UPDATE article SET view_count=view_count+1 WHERE id=?",a.getId());
        assertEquals(before,Db.selectOneBySql("SELECT generation FROM search_sync_task WHERE kind='article' AND target_id=?",a.getId()).getLong("generation"));
        var pool=Executors.newFixedThreadPool(4);
        try{
            var work=new ArrayList<Callable<Integer>>();for(int i=0;i<20;i++){String text=marker+"v"+i;work.add(()->Db.updateBySql("UPDATE article SET content=? WHERE id=?",text,a.getId()));}
            for(var future:pool.invokeAll(work))assertEquals(1,future.get(15,TimeUnit.SECONDS));
        }finally{pool.shutdownNow();}
        assertEquals(before+20,Db.selectOneBySql("SELECT generation FROM search_sync_task WHERE kind='article' AND target_id=?",a.getId()).getLong("generation"));
        String current=Db.selectOneBySql("SELECT content FROM article WHERE id=?",a.getId()).getString("content");
        synchronizedIndex();assertEquals(List.of(a.getId()),ids(search.search(current,"article",1,12)));
    }

    @Test void bulkItemFailureIsReportedAndRetriedInsteadOfAcknowledgedAsSuccess() throws Exception {
        var a=article(marker,1);synchronizedIndex();String physical=store.state().getString("index_name");
        try{
            raw("PUT","/"+physical+"/_settings",Map.of("index.blocks.write",true));
            Db.updateBySql("UPDATE article SET content=? WHERE id=?",marker+"retry",a.getId());index.synchronize();
            var status=index.status();assertTrue(((Number)status.get("pending")).longValue()>0);
            assertTrue(((Number)status.get("failedTasks")).longValue()>0);assertEquals("INDEX_ITEM_REJECTED",status.get("taskError"));
        }finally{raw("PUT","/"+physical+"/_settings",Map.of("index.blocks.write",false));}
        synchronizedIndex();assertEquals(0L,index.status().get("failedTasks"));
        assertEquals(List.of(a.getId()),ids(search.search(marker+"retry","article",1,12)));
    }
}
