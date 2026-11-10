package com.nebula.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.api.search.*;
import com.nebula.common.exception.ResourceLimitException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

/** 真实本机HTTP验证SDK连接保护，不用模拟成功结果替代超时、认证与重定向边界。 */
class ElasticsearchTransportTest {
    private HttpServer server;
    private ExecutorService executor;
    private ElasticsearchGateway gateway;

    private SearchProperties properties()throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        executor=Executors.newCachedThreadPool();server.setExecutor(executor);
        var properties=new SearchProperties();
        properties.setEndpoint("http://127.0.0.1:"+server.getAddress().getPort());
        properties.setUsername("isolated-test");properties.setPassword("isolated-test-only");properties.setRequestTimeoutMs(1000);
        return properties;
    }

    @AfterEach void close() {
        if(gateway!=null)gateway.close();
        if(server!=null)server.stop(0);
        if(executor!=null)executor.shutdownNow();
    }

    @Test void missingIndexIsFalseButAuthenticationFailureIsNotAnEmptyIndex()throws Exception {
        var properties=properties();var status=new AtomicInteger(404);
        server.createContext("/",exchange->{exchange.getResponseHeaders().add("X-Elastic-Product","Elasticsearch");exchange.sendResponseHeaders(status.get(),-1);exchange.close();});server.start();
        gateway=new ElasticsearchGateway(properties,new ObjectMapper());
        assertFalse(gateway.exists("nebula-search"));status.set(401);
        assertThrows(ResourceLimitException.class,()->gateway.exists("nebula-search"));
        assertThrows(IllegalArgumentException.class,()->gateway.exists("unrelated-index"));
    }

    @Test void redirectsCannotForwardCredentialsToAnotherServer()throws Exception {
        var properties=properties();var received=new AtomicInteger();
        var destination=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        destination.createContext("/",exchange->{received.incrementAndGet();exchange.sendResponseHeaders(200,-1);exchange.close();});destination.start();
        try {
            server.createContext("/",exchange->{exchange.getResponseHeaders().add("Location","http://127.0.0.1:"+destination.getAddress().getPort()+"/capture");exchange.sendResponseHeaders(302,-1);exchange.close();});server.start();
            gateway=new ElasticsearchGateway(properties,new ObjectMapper());
            assertThrows(ResourceLimitException.class,()->gateway.exists("nebula-search"));
            assertEquals(0,received.get());
        } finally {destination.stop(0);}
    }

    @Test void slowResponseStreamIsCancelledWithinOverallBudgetAndThePoolRecovers()throws Exception {
        var properties=properties();
        server.createContext("/",exchange->{
            exchange.getResponseHeaders().add("X-Elastic-Product","Elasticsearch");
            if(exchange.getRequestMethod().equals("HEAD")){exchange.sendResponseHeaders(200,-1);exchange.close();return;}
            exchange.getResponseHeaders().add("Content-Type","application/json");exchange.sendResponseHeaders(200,0);
            try(var body=exchange.getResponseBody()) {
                body.write("{\"id\":\"".getBytes(StandardCharsets.UTF_8));body.flush();
                for(int i=0;i<40;i++){body.write('a');body.flush();Thread.sleep(60);}
                body.write("\"}".getBytes(StandardCharsets.UTF_8));
            } catch(java.io.IOException ignored) { }
            catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
            finally {exchange.close();}
        });server.start();gateway=new ElasticsearchGateway(properties,new ObjectMapper());
        assertTrue(gateway.exists("nebula-search"));
        long begin=System.nanoTime();
        assertThrows(ResourceLimitException.class,()->gateway.openPointInTime("nebula-search",200));
        long elapsed=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-begin);
        assertTrue(elapsed<1000,"连续缓慢返回不能绕过200毫秒总预算，实际："+elapsed);
        assertTrue(gateway.exists("nebula-search"));
    }
}
