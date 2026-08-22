package com.example.XsollaTask.review.cache;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public record ReviewCacheClaim(
        CompletableFuture<CachedReviewResult> future,
        boolean owner
) {
    public ReviewCacheClaim {
        Objects.requireNonNull(future);
    }
}