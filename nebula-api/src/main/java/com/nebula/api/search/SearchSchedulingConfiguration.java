package com.nebula.api.search;

import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/** 搜索同步使用独立单线程调度，ES等待不能阻塞定时发布等其他业务轮询。 */
@Configuration
@EnableScheduling
public class SearchSchedulingConfiguration {
    /** 保留原有任务的默认调度线程，避免自定义搜索调度器被框架当作唯一默认调度器。 */
    @Bean(name = "taskScheduler", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "taskScheduler")
    public ThreadPoolTaskScheduler businessScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1); scheduler.setThreadNamePrefix("nebula-scheduled-");
        scheduler.setRemoveOnCancelPolicy(true); scheduler.setWaitForTasksToCompleteOnShutdown(false);
        return scheduler;
    }

    @Bean(name = "nebulaSearchScheduler", destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler searchScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1); scheduler.setThreadNamePrefix("nebula-search-");
        scheduler.setRemoveOnCancelPolicy(true); scheduler.setWaitForTasksToCompleteOnShutdown(false);
        return scheduler;
    }
}
