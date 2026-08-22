package com.example.XsollaTask.review.job;

import com.example.XsollaTask.review.domain.Finding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ReviewJob {

    private final String jobId;

    private JobStatus status;
    private List<Finding> findings;
    private ReviewUsage usage;
    private String failureMessage;
    private final List<ReviewJobEvent> events;
    private long nextEventSequence;
    private final List<ReviewJobEventListener> listeners;

    private ReviewJob(String jobId) {
        this.jobId = Objects.requireNonNull(jobId);
        this.status = JobStatus.QUEUED;
        this.findings = List.of();
        this.events = new ArrayList<>();
        this.nextEventSequence = 1;
        this.listeners = new ArrayList<>();

        appendEvent("status",new StatusEventPayload(JobStatus.QUEUED));
    }

    public static ReviewJob queued(String jobId) {
        return new ReviewJob(jobId);
    }

    public String jobId() {
        return jobId;
    }

    public synchronized void markRunning() {
        requireStatus(JobStatus.QUEUED);
        status = JobStatus.RUNNING;

        appendEvent("status", new StatusEventPayload(status));
    }

    public synchronized void complete(
            List<Finding> findings,
            ReviewUsage usage
    ) {
        requireStatus(JobStatus.RUNNING);

        this.findings = List.copyOf(findings);
        this.usage = Objects.requireNonNull(usage);

        for (Finding finding : this.findings) {
            appendEvent("finding", finding);
        }

        this.status = JobStatus.DONE;

        appendEvent(
                "status",
                new StatusEventPayload(status)
        );

        appendEvent(
                "done",
                new DoneEventPayload(
                        this.findings.size(),
                        usage
                )
        );
        completeListeners();
    }

    public synchronized void fail(String failureMessage) {
        if (status == JobStatus.DONE
                || status == JobStatus.FAILED) {
            throw new IllegalStateException(
                    "A finished job cannot fail"
            );
        }

        if (failureMessage == null
                || failureMessage.isBlank()) {
            throw new IllegalArgumentException(
                    "Failure message must not be blank"
            );
        }

        this.failureMessage = failureMessage;
        this.status = JobStatus.FAILED;

        appendEvent(
                "status",
                new StatusEventPayload(status)
        );
        completeListeners();
    }

    public synchronized ReviewJobSnapshot snapshot() {
        return new ReviewJobSnapshot(
                jobId,
                status,
                findings,
                usage,
                failureMessage
        );
    }

    private void requireStatus(JobStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Expected job status "
                            + expected
                            + " but was "
                            + status
            );
        }
    }
    private void appendEvent(
            String eventName,
            Object data
    ) {
        ReviewJobEvent event = new ReviewJobEvent(
                nextEventSequence,
                eventName,
                data
        );

        nextEventSequence++;
        events.add(event);

        for (
                ReviewJobEventListener listener
                : List.copyOf(listeners)
        ) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException exception) {
                listeners.remove(listener);
            }
        }
    }

    public synchronized List<ReviewJobEvent> events() {
        return List.copyOf(events);
    }

    public synchronized void subscribe(
            ReviewJobEventListener listener
    ) {
        Objects.requireNonNull(listener);

        try {
            for (ReviewJobEvent event : events) {
                listener.onEvent(event);
            }
        } catch (RuntimeException exception) {
            return;
        }

        if (isTerminal()) {
            completeListener(listener);
        } else {
            listeners.add(listener);
        }
    }

    public synchronized void unsubscribe(
            ReviewJobEventListener listener
    ) {
        listeners.remove(listener);
    }

    private boolean isTerminal() {
        return status == JobStatus.DONE
                || status == JobStatus.FAILED;
    }

    private void completeListeners() {
        for (ReviewJobEventListener listener : List.copyOf(listeners))
        {
            completeListener(listener);
        }

        listeners.clear();
    }

    private void completeListener(ReviewJobEventListener listener)
    {
        try {
            listener.onComplete();
        } catch (RuntimeException ignored) {
            // The client may already have disconnected.
        }
    }
}