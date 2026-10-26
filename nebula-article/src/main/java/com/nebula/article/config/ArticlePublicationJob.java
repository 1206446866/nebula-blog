package com.nebula.article.config;

import com.nebula.article.service.ArticlePublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** 数据库持久化轮询，停机期间到期的计划在恢复后继续执行。 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nebula.publication.enabled", havingValue = "true", matchIfMissing = true)
public class ArticlePublicationJob {
    private final ArticlePublicationService publications;

    @Scheduled(fixedDelayString = "${nebula.publication.poll-delay-ms:15000}", initialDelayString = "${nebula.publication.initial-delay-ms:15000}")
    public void publish() { publications.publishDue(); }
}
