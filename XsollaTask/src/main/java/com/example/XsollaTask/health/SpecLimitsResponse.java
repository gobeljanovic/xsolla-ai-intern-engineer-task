package com.example.XsollaTask.health;

public record SpecLimitsResponse(
        int maxPayloadBytes,
        int chunkBytes,
        int maxConcurrentJobs,
        int rateLimitPerMinute
) {
}