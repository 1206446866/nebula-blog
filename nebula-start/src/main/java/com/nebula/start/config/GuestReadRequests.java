package com.nebula.start.config;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;
import java.util.stream.Stream;

/** 统一维护游客读取入口，授权与匿名读取预算使用同一组请求匹配规则。 */
final class GuestReadRequests {
    private static final List<String> PATHS = List.of(
            "/articles/published", "/articles/{id:[0-9]+}",
            "/categories", "/categories/list", "/tags", "/tags/list",
            "/comments/threads", "/comments/{rootId:[0-9]+}/replies");

    static final RequestMatcher MATCHER = new OrRequestMatcher(
            Stream.of(HttpMethod.GET, HttpMethod.HEAD)
                    .flatMap(method -> PATHS.stream()
                            .map(path -> PathPatternRequestMatcher.withDefaults().matcher(method, path)))
                    .toList());

    private GuestReadRequests() {
    }
}
