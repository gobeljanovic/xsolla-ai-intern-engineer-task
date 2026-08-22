package com.example.XsollaTask.review.job;

public record ReviewUsage(
        int inputBytes,
        int chunks,
        boolean cacheHit
) {
    public ReviewUsage {
        if (inputBytes < 0) {
            throw new IllegalArgumentException(
                    "inputBytes must not be negative"
            );
        }

        if (chunks < 1) {
            throw new IllegalArgumentException(
                    "chunks must be positive"
            );
        }
    }
}