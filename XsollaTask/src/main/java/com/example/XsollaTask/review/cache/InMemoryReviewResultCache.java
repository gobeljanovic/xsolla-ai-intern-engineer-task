package com.example.XsollaTask.review.cache;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public final class InMemoryReviewResultCache {

    private final ConcurrentMap<
            ReviewCacheKey,
            CompletableFuture<CachedReviewResult>
            > entries = new ConcurrentHashMap<>();

    public ReviewCacheClaim claim(ReviewCacheKey key) {
        Objects.requireNonNull(key);

        CompletableFuture<CachedReviewResult> candidate =
                new CompletableFuture<>();

        CompletableFuture<CachedReviewResult> existing =
                entries.putIfAbsent(key, candidate);

        if (existing == null) {
            return new ReviewCacheClaim(
                    candidate,
                    true
            );
        }

        return new ReviewCacheClaim(
                existing,
                false
        );
    }

    public void complete(
            ReviewCacheClaim claim,
            CachedReviewResult result
    ) {
        claim.future().complete(result);
    }

    public void fail(
            ReviewCacheKey key,
            ReviewCacheClaim claim,
            Throwable failure
    ) {
        entries.remove(key, claim.future());
        claim.future().completeExceptionally(failure);
    }
}