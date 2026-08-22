package com.example.XsollaTask.review.cache;

import com.example.XsollaTask.review.provider.ProviderType;

import java.util.Objects;

public record ReviewCacheKey(
        String diff,
        ProviderType provider,
        int maxFindings
) {
    public ReviewCacheKey {
        Objects.requireNonNull(diff);
        Objects.requireNonNull(provider);

        if (maxFindings < 0) {
            throw new IllegalArgumentException(
                    "maxFindings must not be negative"
            );
        }
    }
}