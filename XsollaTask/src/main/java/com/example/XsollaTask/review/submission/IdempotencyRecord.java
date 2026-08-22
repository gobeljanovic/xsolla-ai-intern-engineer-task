package com.example.XsollaTask.review.submission;

record IdempotencyRecord(
        String bodyFingerprint,
        String jobId
) {
}