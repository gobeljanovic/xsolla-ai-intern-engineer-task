package com.example.XsollaTask.review.job;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executor;

@Service
public final class ReviewStreamService {

    private final InMemoryReviewJobStore jobStore;
    private final Executor sseExecutor;

    public ReviewStreamService(
            InMemoryReviewJobStore jobStore,
            @Qualifier("sseExecutor") Executor sseExecutor
    ) {
        this.jobStore = Objects.requireNonNull(jobStore);
        this.sseExecutor = sseExecutor;
    }

    public SseEmitter stream(String jobId) {
        ReviewJob job = jobStore.findJob(jobId)
                .orElseThrow(() ->
                        new ReviewJobNotFoundException(jobId)
                );

        // Zero means no framework timeout.
        SseEmitter emitter = new SseEmitter(0L);

        SseSubscriber subscriber =
                new SseSubscriber(emitter);

        emitter.onCompletion(() ->
                job.unsubscribe(subscriber)
        );

        emitter.onTimeout(() -> {
            job.unsubscribe(subscriber);
            emitter.complete();
        });

        emitter.onError(exception ->
                job.unsubscribe(subscriber)
        );

        job.subscribe(subscriber);

        return emitter;
    }

    private static final class SseSubscriber
            implements ReviewJobEventListener {

        private final SseEmitter emitter;

        private SseSubscriber(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onEvent(ReviewJobEvent event) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .id(Long.toString(
                                        event.sequence()
                                ))
                                .name(event.eventName())
                                .data(
                                        event.data(),
                                        MediaType.APPLICATION_JSON
                                )
                );
            } catch (IOException exception) {
                emitter.completeWithError(exception);

                throw new IllegalStateException(
                        "Failed to send SSE event",
                        exception
                );
            }
        }

        @Override
        public void onComplete() {
            emitter.complete();
        }
    }
}