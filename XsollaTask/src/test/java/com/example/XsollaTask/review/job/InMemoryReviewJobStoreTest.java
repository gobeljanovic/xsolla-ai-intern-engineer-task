package com.example.XsollaTask.review.job;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryReviewJobStoreTest {

    private final InMemoryReviewJobStore store =
            new InMemoryReviewJobStore();

    @Test
    void savesAndReturnsJobSnapshot() {
        ReviewJob job = ReviewJob.queued("job-1");

        store.save(job);

        assertThat(store.findSnapshot("job-1"))
                .hasValueSatisfying(snapshot -> {
                    assertThat(snapshot.jobId())
                            .isEqualTo("job-1");

                    assertThat(snapshot.status())
                            .isEqualTo(JobStatus.QUEUED);
                });
    }

    @Test
    void returnsEmptyForUnknownJob() {
        assertThat(store.findSnapshot("missing"))
                .isEmpty();
    }

    @Test
    void doesNotOverwriteDuplicateJobId() {
        ReviewJob first =
                ReviewJob.queued("job-1");

        ReviewJob duplicate =
                ReviewJob.queued("job-1");

        store.save(first);

        assertThatThrownBy(() -> store.save(duplicate))
                .isInstanceOf(IllegalStateException.class);

        assertThat(store.findJob("job-1"))
                .contains(first);
    }
}