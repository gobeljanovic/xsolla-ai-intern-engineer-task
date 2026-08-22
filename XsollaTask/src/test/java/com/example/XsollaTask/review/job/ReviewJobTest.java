package com.example.XsollaTask.review.job;
import com.example.XsollaTask.review.domain.Category;
import com.example.XsollaTask.review.domain.Finding;
import com.example.XsollaTask.review.domain.Severity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReviewJobTest {

    @Test
    void startsQueuedAndCompletesThroughRunning() {
        ReviewJob job = ReviewJob.queued("job-1");

        assertThat(job.snapshot().status())
                .isEqualTo(JobStatus.QUEUED);

        job.markRunning();

        ReviewUsage usage =
                new ReviewUsage(100, 2, false);

        job.complete(List.of(), usage);

        ReviewJobSnapshot snapshot = job.snapshot();

        assertThat(snapshot.status())
                .isEqualTo(JobStatus.DONE);

        assertThat(snapshot.usage())
                .isEqualTo(usage);
    }

    @Test
    void cannotCompleteBeforeRunning() {
        ReviewJob job = ReviewJob.queued("job-1");

        assertThatThrownBy(() -> job.complete(
                List.of(),
                new ReviewUsage(100, 1, false)
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void canFailWhileRunning() {
        ReviewJob job = ReviewJob.queued("job-1");
        job.markRunning();
        job.fail("Provider unavailable");

        ReviewJobSnapshot snapshot = job.snapshot();

        assertThat(snapshot.status())
                .isEqualTo(JobStatus.FAILED);

        assertThat(snapshot.failureMessage())
                .isEqualTo("Provider unavailable");
    }

    @Test
    void terminalJobCannotTransitionAgain() {
        ReviewJob job = ReviewJob.queued("job-1");
        job.markRunning();
        job.complete(
                List.of(),
                new ReviewUsage(100, 1, false)
        );

        assertThatThrownBy(job::markRunning)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recordsSuccessfulEventSequence() {
        ReviewJob job = ReviewJob.queued("job-1");

        Finding finding = Finding.create(
                "MOCK-001",
                "src/app.js",
                10,
                Severity.CRITICAL,
                Category.SECURITY,
                "eval usage",
                "eval(input);"
        );

        ReviewUsage usage =
                new ReviewUsage(100, 1, false);

        job.markRunning();
        job.complete(List.of(finding), usage);

        List<ReviewJobEvent> events = job.events();

        assertThat(events)
                .extracting(ReviewJobEvent::eventName)
                .containsExactly(
                        "status",
                        "status",
                        "finding",
                        "status",
                        "done"
                );

        assertThat(events)
                .extracting(ReviewJobEvent::sequence)
                .containsExactly(1L, 2L, 3L, 4L, 5L);

        assertThat(events.get(0).data())
                .isEqualTo(
                        new StatusEventPayload(
                                JobStatus.QUEUED
                        )
                );

        assertThat(events.get(2).data())
                .isEqualTo(finding);

        assertThat(events.get(4).data())
                .isEqualTo(
                        new DoneEventPayload(1, usage)
                );
    }

    @Test
    void recordsFailedStatusEvent() {
        ReviewJob job = ReviewJob.queued("job-1");

        job.markRunning();
        job.fail("Provider unavailable");

        assertThat(job.events())
                .extracting(ReviewJobEvent::eventName)
                .containsExactly(
                        "status",
                        "status",
                        "status"
                );

        assertThat(job.events().get(2).data())
                .isEqualTo(
                        new StatusEventPayload(
                                JobStatus.FAILED
                        )
                );
    }
    @Test
    void liveAndReplaySubscribersReceiveIdenticalEvents() {
        ReviewJob job = ReviewJob.queued("job-1");

        Finding finding = Finding.create(
                "MOCK-001",
                "src/app.js",
                10,
                Severity.CRITICAL,
                Category.SECURITY,
                "eval usage",
                "eval(input);"
        );

        ReviewUsage usage =  new ReviewUsage(100, 1, false);

        RecordingListener live = new RecordingListener();

        job.subscribe(live);

        job.markRunning();
        job.complete(List.of(finding), usage);

        RecordingListener replay =
                new RecordingListener();

        // The job is already finished, so this replays everything.
        job.subscribe(replay);

        assertThat(replay.events)
                .containsExactlyElementsOf(live.events);

        assertThat(live.completed).isTrue();
        assertThat(replay.completed).isTrue();
    }

    private static final class RecordingListener
            implements ReviewJobEventListener {

        private final List<ReviewJobEvent> events =
                new ArrayList<>();

        private boolean completed;

        @Override
        public void onEvent(ReviewJobEvent event) {
            events.add(event);
        }

        @Override
        public void onComplete() {
            completed = true;
        }
    }
}
