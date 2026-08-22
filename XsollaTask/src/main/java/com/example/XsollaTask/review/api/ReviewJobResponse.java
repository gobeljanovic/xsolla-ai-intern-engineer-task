package com.example.XsollaTask.review.api;

import com.example.XsollaTask.review.domain.Finding;
import com.example.XsollaTask.review.job.JobStatus;
import com.example.XsollaTask.review.job.ReviewJobSnapshot;
import com.example.XsollaTask.review.job.ReviewUsage;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewJobResponse(
        String jobId,
        JobStatus status,
        List<Finding> findings,
        ReviewUsage usage,
        String failureMessage
) {
    public static ReviewJobResponse from(
            ReviewJobSnapshot snapshot
    ) {
        boolean done =
                snapshot.status() == JobStatus.DONE;

        boolean failed =
                snapshot.status() == JobStatus.FAILED;

        return new ReviewJobResponse(
                snapshot.jobId(),
                snapshot.status(),
                done ? snapshot.findings() : null,
                done ? snapshot.usage() : null,
                failed ? snapshot.failureMessage() : null
        );
    }
}