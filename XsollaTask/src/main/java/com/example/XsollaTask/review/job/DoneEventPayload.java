package com.example.XsollaTask.review.job;

import java.util.Objects;

public record DoneEventPayload(
        int total,
        ReviewUsage usage
) {
    public DoneEventPayload {
        if (total < 0) {
            throw new IllegalArgumentException(
                    "Total must not be negative"
            );
        }

        Objects.requireNonNull(usage);
    }
}