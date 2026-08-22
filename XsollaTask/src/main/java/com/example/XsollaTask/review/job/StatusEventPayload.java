package com.example.XsollaTask.review.job;

import java.util.Objects;

public record StatusEventPayload(
        JobStatus status
) {
    public StatusEventPayload {
        Objects.requireNonNull(status);
    }
}