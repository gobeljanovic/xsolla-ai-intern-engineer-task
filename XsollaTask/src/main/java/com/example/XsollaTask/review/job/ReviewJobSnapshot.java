package com.example.XsollaTask.review.job;

import com.example.XsollaTask.review.domain.Finding;

import java.util.List;
import java.util.Objects;

public record ReviewJobSnapshot(
        String jobId,
        JobStatus status,
        List<Finding> findings,
        ReviewUsage usage,
        String failureMessage
) {
    public ReviewJobSnapshot {
        Objects.requireNonNull(jobId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(findings);

        findings = List.copyOf(findings);
    }
}