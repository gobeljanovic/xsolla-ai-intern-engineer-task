package com.example.XsollaTask.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ReviewExecutorConfig {

    @Bean(
            name = "reviewExecutor",
            destroyMethod = "shutdown"
    )
    public ExecutorService reviewExecutor(
            LimitsProperties limits
    ) {
        return Executors.newFixedThreadPool(
                limits.maxConcurrentJobs()
        );
    }

    @Bean(name = "sseExecutor", destroyMethod = "shutdown")
    public ExecutorService sseExecutor() {
        return Executors.newCachedThreadPool();
    }
}