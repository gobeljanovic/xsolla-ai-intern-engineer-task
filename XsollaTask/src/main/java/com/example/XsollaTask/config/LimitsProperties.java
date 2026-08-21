package com.example.XsollaTask.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.limits")
@Validated
public record LimitsProperties(
        @Positive int maxPayloadBytes,
        @Positive int chunkBytes,
        @Positive int maxConcurrentJobs,
        @Positive int rateLimitPerMinute
) {
}