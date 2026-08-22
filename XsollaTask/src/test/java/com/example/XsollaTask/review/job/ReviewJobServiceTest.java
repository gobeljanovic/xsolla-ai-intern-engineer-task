package com.example.XsollaTask.review.job;

import com.example.XsollaTask.config.LimitsProperties;
import com.example.XsollaTask.review.cache.InMemoryReviewResultCache;
import com.example.XsollaTask.review.cache.ReviewCacheKey;
import com.example.XsollaTask.review.diff.DiffFile;
import com.example.XsollaTask.review.diff.ParsedDiff;
import com.example.XsollaTask.review.domain.Category;
import com.example.XsollaTask.review.domain.Finding;
import com.example.XsollaTask.review.domain.Severity;
import com.example.XsollaTask.review.pipeline.DiffChunker;
import com.example.XsollaTask.review.pipeline.ReviewPipeline;
import com.example.XsollaTask.review.provider.ProviderType;
import com.example.XsollaTask.review.provider.ReviewProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewJobServiceTest {

    private final InMemoryReviewJobStore store =
            new InMemoryReviewJobStore();

    private final CapturingExecutor executor =
            new CapturingExecutor();

    private final ReviewJobService service =
            new ReviewJobService(
                    store,
                    pipeline(),
                    executor,
                    new InMemoryReviewResultCache()
            );

    @Test
    void queuesAndCompletesJobAsynchronously() {
        Finding finding = Finding.create(
                "MOCK-001",
                "src/app.js",
                10,
                Severity.CRITICAL,
                Category.SECURITY,
                "eval usage",
                "eval(input);"
        );

        ReviewProvider provider =
                ignored -> List.of(finding);

        String jobId = service.submit(
                provider,
                parsedDiff(),
                100,
                123,
                new ReviewCacheKey(
                        "request-diff",
                        ProviderType.MOCK,
                        100
                )
        );

        assertThat(service.get(jobId).status())
                .isEqualTo(JobStatus.QUEUED);

        executor.runCapturedTask();

        ReviewJobSnapshot completed =
                service.get(jobId);

        assertThat(completed.status())
                .isEqualTo(JobStatus.DONE);

        assertThat(completed.findings())
                .containsExactly(finding);

        assertThat(completed.usage())
                .isEqualTo(new ReviewUsage(
                        123,
                        1,
                        false
                ));
    }

    @Test
    void providerFailureMarksJobAsFailed() {
        ReviewProvider provider = ignored -> {
            throw new IllegalStateException(
                    "LLM unavailable"
            );
        };

        String jobId = service.submit(
                provider,
                parsedDiff(),
                100,
                123,
                new ReviewCacheKey(
                        "request-diff",
                        ProviderType.MOCK,
                        100
                )
        );

        executor.runCapturedTask();

        ReviewJobSnapshot failed =
                service.get(jobId);

        assertThat(failed.status())
                .isEqualTo(JobStatus.FAILED);

        assertThat(failed.failureMessage())
                .contains("LLM unavailable");
    }

    private ReviewPipeline pipeline() {
        LimitsProperties limits = new LimitsProperties(
                1_048_576,
                65_536,
                4,
                30
        );

        return new ReviewPipeline(
                new DiffChunker(limits)
        );
    }

    private ParsedDiff parsedDiff() {
        return new ParsedDiff(List.of(
                new DiffFile(
                        "src/app.js",
                        "1234",
                        List.of()
                )
        ));
    }

    private static final class CapturingExecutor
            implements Executor {

        private Runnable capturedTask;

        @Override
        public void execute(Runnable command) {
            capturedTask = command;
        }

        void runCapturedTask() {
            if (capturedTask == null) {
                throw new IllegalStateException(
                        "No task was captured"
                );
            }

            Runnable task = capturedTask;
            capturedTask = null;
            task.run();
        }
    }

    @Test
    void reusesCompletedResultForIdenticalInput() {
        AtomicInteger providerCalls =
                new AtomicInteger();

        ReviewProvider provider = ignored -> {
            providerCalls.incrementAndGet();
            return List.of();
        };

        ReviewCacheKey key = new ReviewCacheKey(
                "same-diff",
                ProviderType.MOCK,
                100
        );

        String firstJobId = service.submit(
                provider,
                parsedDiff(),
                100,
                123,
                key
        );

        executor.runCapturedTask();

        String secondJobId = service.submit(
                provider,
                parsedDiff(),
                100,
                123,
                key
        );

        ReviewJobSnapshot first = service.get(firstJobId);
        ReviewJobSnapshot second = service.get(secondJobId);

        assertThat(secondJobId).isNotEqualTo(firstJobId);
        assertThat(providerCalls.get()).isEqualTo(1);
        assertThat(first.usage().cacheHit()).isFalse();
        assertThat(second.usage().cacheHit()).isTrue();
        assertThat(second.findings())
                .isEqualTo(first.findings());
    }
}