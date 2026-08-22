package com.example.XsollaTask.review.job;

public final class ReviewJobNotFoundException
        extends RuntimeException {

    public ReviewJobNotFoundException(String jobId) {
        super("Review job not found: " + jobId);
    }
}