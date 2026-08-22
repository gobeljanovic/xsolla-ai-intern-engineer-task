package com.example.XsollaTask.review.api;

import com.example.XsollaTask.review.job.ReviewStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;

@RestController
@RequestMapping("/v1/reviews")
public final class ReviewStreamController {

    private final ReviewStreamService streamService;

    public ReviewStreamController(
            ReviewStreamService streamService
    ) {
        this.streamService =
                Objects.requireNonNull(streamService);
    }

    @GetMapping(
            value = "/{jobId}/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter stream(
            @PathVariable String jobId
    ) {
        return streamService.stream(jobId);
    }
}