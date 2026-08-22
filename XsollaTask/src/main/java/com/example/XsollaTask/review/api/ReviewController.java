package com.example.XsollaTask.review.api;

import com.example.XsollaTask.common.api.InvalidJsonException;
import com.example.XsollaTask.review.cache.ReviewCacheKey;
import com.example.XsollaTask.review.diff.InvalidDiffException;
import com.example.XsollaTask.review.diff.ParsedDiff;
import com.example.XsollaTask.review.diff.UnifiedDiffParser;
import com.example.XsollaTask.review.job.ReviewJobService;
import com.example.XsollaTask.review.job.ReviewJobSnapshot;
import com.example.XsollaTask.review.provider.ProviderType;
import com.example.XsollaTask.review.provider.ReviewProvider;
import com.example.XsollaTask.review.provider.ReviewProviderRegistry;
import com.example.XsollaTask.review.submission.InMemoryIdempotencyStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/v1/reviews")
public final class ReviewController {

    private final UnifiedDiffParser diffParser;
    private final ReviewProviderRegistry providerRegistry;
    private final ReviewJobService jobService;
    private final ObjectMapper objectMapper;
    private final InMemoryIdempotencyStore idempotencyStore;

    public ReviewController(
            UnifiedDiffParser diffParser,
            ReviewProviderRegistry providerRegistry,
            ReviewJobService jobService,
            ObjectMapper objectMapper,
            InMemoryIdempotencyStore idempotencyStore
    ) {
        this.diffParser = diffParser;
        this.providerRegistry = providerRegistry;
        this.jobService = jobService;
        this.objectMapper = objectMapper;
        this.idempotencyStore = idempotencyStore;
    }

    @PostMapping
    public ResponseEntity<CreateReviewResponse> create(
            @RequestHeader(
                    name = "Idempotency-Key",
                    required = false
            )
            String idempotencyKey,
            @RequestBody byte[] rawBody
    ) {
        CreateReviewRequest request =
                readRequest(rawBody);

        if (request == null) {
            throw new InvalidDiffException(
                    "Diff must not be empty"
            );
        }

        ParsedDiff parsedDiff =
                diffParser.parse(request.diff());

        ProviderType providerType =
                request.effectiveProvider();

        int maxFindings =
                request.effectiveMaxFindings();

        ReviewProvider provider =
                providerRegistry.resolve(providerType);

        int inputBytes = request.diff()
                .getBytes(StandardCharsets.UTF_8)
                .length;

        ReviewCacheKey cacheKey =
                new ReviewCacheKey(
                        request.diff(),
                        providerType,
                        maxFindings
                );

        String jobId = idempotencyStore.resolve(
                idempotencyKey,
                rawBody,
                () -> jobService.submit(
                        provider,
                        parsedDiff,
                        maxFindings,
                        inputBytes,
                        cacheKey
                )
        );

        return ResponseEntity
                .accepted()
                .body(
                        CreateReviewResponse.queued(
                                jobId
                        )
                );
    }

    @GetMapping("/{jobId}")
    public ReviewJobResponse get(
            @PathVariable String jobId
    ) {
        ReviewJobSnapshot snapshot =
                jobService.get(jobId);

        return ReviewJobResponse.from(snapshot);
    }

    private CreateReviewRequest readRequest(
            byte[] rawBody
    ) {
        try {
            return objectMapper.readValue(
                    rawBody,
                    CreateReviewRequest.class
            );
        } catch (IOException exception) {
            throw new InvalidJsonException();
        }
    }
}