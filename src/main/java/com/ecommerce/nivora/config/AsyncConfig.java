package com.ecommerce.nivora.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Dedicated thread pool for async tasks (FastAPI calls, indexing, etc.).
 * Using a named executor instead of ForkJoinPool.commonPool to:
 * - Prevent starvation of JVM's shared pool
 * - Allow bounded queue with CallerRunsPolicy as backpressure
 * - Enable proper monitoring and graceful shutdown
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-task-");
        executor.setKeepAliveSeconds(60);
        // CallerRunsPolicy: if queue full, calling thread runs the task instead of dropping it
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("Async executor initialized: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }
}
