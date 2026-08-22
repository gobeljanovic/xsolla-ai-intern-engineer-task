package com.example.XsollaTask.review.job;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public final class InMemoryReviewJobStore {

    private final ConcurrentMap<String, ReviewJob> jobs =
            new ConcurrentHashMap<>();

    public void save(ReviewJob job) {
        Objects.requireNonNull(job);

        ReviewJob existing = jobs.putIfAbsent(
                job.jobId(),
                job
        );

        if (existing != null) {
            throw new IllegalStateException(
                    "Job ID already exists: " + job.jobId()
            );
        }
    }

    public Optional<ReviewJob> findJob(String jobId) {
        Objects.requireNonNull(jobId);
        return Optional.ofNullable(jobs.get(jobId));
    }

    public Optional<ReviewJobSnapshot> findSnapshot(
            String jobId
    ) {
        return findJob(jobId)
                .map(ReviewJob::snapshot);
    }
}