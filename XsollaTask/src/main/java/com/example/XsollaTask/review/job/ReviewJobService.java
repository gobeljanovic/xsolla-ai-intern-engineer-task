package com.example.XsollaTask.review.job;

import com.example.XsollaTask.review.cache.CachedReviewResult;
import com.example.XsollaTask.review.cache.InMemoryReviewResultCache;
import com.example.XsollaTask.review.cache.ReviewCacheClaim;
import com.example.XsollaTask.review.cache.ReviewCacheKey;
import com.example.XsollaTask.review.diff.ParsedDiff;
import com.example.XsollaTask.review.pipeline.ReviewPipeline;
import com.example.XsollaTask.review.pipeline.ReviewPipelineResult;
import com.example.XsollaTask.review.provider.ReviewProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
public final class ReviewJobService {

    private final InMemoryReviewJobStore jobStore;
    private final ReviewPipeline reviewPipeline;
    private final Executor reviewExecutor;
    private final InMemoryReviewResultCache resultCache;

    public ReviewJobService(
            InMemoryReviewJobStore jobStore,
            ReviewPipeline reviewPipeline,
            @Qualifier("reviewExecutor")
            Executor reviewExecutor,
            InMemoryReviewResultCache resultCache
    ) {
        this.jobStore =
                Objects.requireNonNull(jobStore);

        this.reviewPipeline =
                Objects.requireNonNull(reviewPipeline);

        this.reviewExecutor =
                Objects.requireNonNull(reviewExecutor);

        this.resultCache =
                Objects.requireNonNull(resultCache);
    }

    public String submit(
            ReviewProvider provider,
            ParsedDiff diff,
            int maxFindings,
            int inputBytes,
            ReviewCacheKey cacheKey
    ) {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(diff);
        Objects.requireNonNull(cacheKey);

        String jobId = UUID.randomUUID().toString();
        ReviewJob job = ReviewJob.queued(jobId);

        jobStore.save(job);

        ReviewCacheClaim claim =
                resultCache.claim(cacheKey);

        if (claim.owner()) {
            scheduleReview(
                    job,
                    provider,
                    diff,
                    maxFindings,
                    inputBytes,
                    cacheKey,
                    claim
            );
        } else {
            reuseCachedResult(
                    job,
                    inputBytes,
                    claim
            );
        }

        return jobId;
    }

    public ReviewJobSnapshot get(String jobId) {
        return jobStore.findSnapshot(jobId)
                .orElseThrow(
                        () -> new ReviewJobNotFoundException(
                                jobId
                        )
                );
    }

    private void scheduleReview(
            ReviewJob job,
            ReviewProvider provider,
            ParsedDiff diff,
            int maxFindings,
            int inputBytes,
            ReviewCacheKey cacheKey,
            ReviewCacheClaim claim
    ) {
        try {
            reviewExecutor.execute(
                    () -> process(
                            job,
                            provider,
                            diff,
                            maxFindings,
                            inputBytes,
                            cacheKey,
                            claim
                    )
            );
        } catch (RuntimeException exception) {
            resultCache.fail(
                    cacheKey,
                    claim,
                    exception
            );

            job.fail(
                    "Unable to schedule review job"
            );

            throw exception;
        }
    }

    private void process(
            ReviewJob job,
            ReviewProvider provider,
            ParsedDiff diff,
            int maxFindings,
            int inputBytes,
            ReviewCacheKey cacheKey,
            ReviewCacheClaim claim
    ) {
        try {
            job.markRunning();

            ReviewPipelineResult result =
                    reviewPipeline.execute(
                            provider,
                            diff,
                            maxFindings
                    );

            ReviewUsage usage = new ReviewUsage(
                    inputBytes,
                    result.chunks(),
                    false
            );

            job.complete(
                    result.findings(),
                    usage
            );

            CachedReviewResult cachedResult =
                    new CachedReviewResult(
                            result.findings(),
                            result.chunks()
                    );

            resultCache.complete(
                    claim,
                    cachedResult
            );
        } catch (Exception exception) {
            resultCache.fail(
                    cacheKey,
                    claim,
                    exception
            );

            job.fail(
                    failureMessage(exception)
            );
        }
    }

    private void reuseCachedResult(
            ReviewJob job,
            int inputBytes,
            ReviewCacheClaim claim
    ) {
        job.markRunning();

        claim.future().whenComplete(
                (cachedResult, failure) -> {
                    if (failure != null) {
                        job.fail(
                                failureMessage(failure)
                        );
                        return;
                    }

                    ReviewUsage usage =
                            new ReviewUsage(
                                    inputBytes,
                                    cachedResult.chunks(),
                                    true
                            );

                    job.complete(
                            cachedResult.findings(),
                            usage
                    );
                }
        );
    }

    private String failureMessage(
            Throwable failure
    ) {
        Throwable actual = failure;

        if (failure instanceof CompletionException
                && failure.getCause() != null) {
            actual = failure.getCause();
        }

        String detail = actual.getMessage();

        if (detail == null || detail.isBlank()) {
            return "Review processing failed";
        }

        return "Review processing failed: " + detail;
    }
}