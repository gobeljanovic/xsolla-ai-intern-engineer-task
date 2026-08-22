package com.example.XsollaTask.review.job;

import java.util.Objects;

public record ReviewJobEvent(
        long sequence,
        String eventName,
        Object data
) {
    public ReviewJobEvent {
        if (sequence < 1) {
            throw new IllegalArgumentException(
                    "Event sequence must be positive"
            );
        }

        Objects.requireNonNull(eventName);
        Objects.requireNonNull(data);
    }
}