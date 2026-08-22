package com.example.XsollaTask.review.api;

import com.example.XsollaTask.review.job.JobStatus;

public record CreateReviewResponse(
        String jobId,
        JobStatus status
) {
    public static CreateReviewResponse queued(
            String jobId
    ) {
        return new CreateReviewResponse(
                jobId,
                JobStatus.QUEUED
        );
    }
}