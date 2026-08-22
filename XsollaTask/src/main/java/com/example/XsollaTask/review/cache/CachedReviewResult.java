package com.example.XsollaTask.review.cache;

import com.example.XsollaTask.review.domain.Finding;

import java.util.List;
import java.util.Objects;

public record CachedReviewResult(
        List<Finding> findings,
        int chunks
) {
    public CachedReviewResult {
        Objects.requireNonNull(findings);
        findings = List.copyOf(findings);

        if (chunks < 1) {
            throw new IllegalArgumentException(
                    "Chunk count must be positive"
            );
        }
    }
}