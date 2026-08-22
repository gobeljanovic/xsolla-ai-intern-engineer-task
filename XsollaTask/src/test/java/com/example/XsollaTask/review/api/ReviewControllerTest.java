package com.example.XsollaTask.review.api;

import com.example.XsollaTask.common.api.ApiExceptionHandler;
import com.example.XsollaTask.review.cache.ReviewCacheKey;
import com.example.XsollaTask.review.diff.DiffFile;
import com.example.XsollaTask.review.diff.InvalidDiffException;
import com.example.XsollaTask.review.diff.ParsedDiff;
import com.example.XsollaTask.review.diff.UnifiedDiffParser;
import com.example.XsollaTask.review.domain.Category;
import com.example.XsollaTask.review.domain.Finding;
import com.example.XsollaTask.review.domain.Severity;
import com.example.XsollaTask.review.job.JobStatus;
import com.example.XsollaTask.review.job.ReviewJobNotFoundException;
import com.example.XsollaTask.review.job.ReviewJobService;
import com.example.XsollaTask.review.job.ReviewJobSnapshot;
import com.example.XsollaTask.review.job.ReviewUsage;
import com.example.XsollaTask.review.provider.ProviderType;
import com.example.XsollaTask.review.provider.ReviewProvider;
import com.example.XsollaTask.review.provider.ReviewProviderRegistry;
import com.example.XsollaTask.review.submission.InMemoryIdempotencyStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ReviewControllerTest {

    private UnifiedDiffParser diffParser;
    private ReviewProviderRegistry providerRegistry;
    private ReviewJobService jobService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        diffParser = mock(UnifiedDiffParser.class);
        providerRegistry = mock(ReviewProviderRegistry.class);
        jobService = mock(ReviewJobService.class);

        // Must be initialized before passing it to the controller.
        objectMapper = new ObjectMapper();

        ReviewController controller =
                new ReviewController(
                        diffParser,
                        providerRegistry,
                        jobService,
                        objectMapper,
                        new InMemoryIdempotencyStore()
                );

        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }
    @Test
    void createsQueuedMockReviewWithDefaults() throws Exception {

        String diff = validDiff();
        ParsedDiff parsedDiff = parsedDiff(diff);

        ReviewProvider provider =
                ignored -> List.of();

        when(diffParser.parse(diff))
                .thenReturn(parsedDiff);

        when(providerRegistry.resolve(
                ProviderType.MOCK
        )).thenReturn(provider);

        int inputBytes = diff
                .getBytes(StandardCharsets.UTF_8)
                .length;

        when(jobService.submit(
                same(provider),
                same(parsedDiff),
                eq(100),
                eq(inputBytes),
                eq(new ReviewCacheKey(
                        diff,
                        ProviderType.MOCK,
                        100
                ))
        )).thenReturn("job-123");

        String body = objectMapper.writeValueAsString(
                Map.of(
                        "diff", diff,
                        "unknownField", "ignored"
                )
        );

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.jobId")
                        .value("job-123"))
                .andExpect(jsonPath("$.status")
                        .value("queued"));

        verify(jobService).submit(
                same(provider),
                same(parsedDiff),
                eq(100),
                eq(inputBytes),
                eq(new ReviewCacheKey(
                        diff,
                        ProviderType.MOCK,
                        100
                ))
        );
    }

    @Test
    void acceptsExplicitLlmOptions()
            throws Exception {

        String diff = validDiff();
        ParsedDiff parsedDiff = parsedDiff(diff);

        ReviewProvider provider =
                ignored -> List.of();

        when(diffParser.parse(diff))
                .thenReturn(parsedDiff);

        when(providerRegistry.resolve(
                ProviderType.LLM
        )).thenReturn(provider);

        int inputBytes = diff
                .getBytes(StandardCharsets.UTF_8)
                .length;

        when(jobService.submit(
                same(provider),
                same(parsedDiff),
                eq(25),
                eq(inputBytes),
                eq(new ReviewCacheKey(
                        diff,
                        ProviderType.LLM,
                        25
                ))
        )).thenReturn("job-llm");

        String body = objectMapper.writeValueAsString(
                Map.of(
                        "diff", diff,
                        "options", Map.of(
                                "provider", "llm",
                                "maxFindings", 25
                        )
                )
        );

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId")
                        .value("job-llm"))
                .andExpect(jsonPath("$.status")
                        .value("queued"));
    }

    @Test
    void returnsCompletedJobWithFindingsAndUsage()
            throws Exception {

        Finding finding = Finding.create(
                "MOCK-001",
                "src/app.js",
                11,
                Severity.CRITICAL,
                Category.SECURITY,
                "eval usage",
                "eval(input);"
        );

        ReviewJobSnapshot snapshot =
                new ReviewJobSnapshot(
                        "job-123",
                        JobStatus.DONE,
                        List.of(finding),
                        new ReviewUsage(100,2,false),
                        null
                );

        when(jobService.get("job-123"))
                .thenReturn(snapshot);

        mockMvc.perform(get("/v1/reviews/job-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId")
                        .value("job-123"))
                .andExpect(jsonPath("$.status")
                        .value("done"))
                .andExpect(jsonPath("$.findings[0].id")
                        .value("MOCK-001:src/app.js:11"))
                .andExpect(jsonPath("$.findings[0].severity")
                        .value("critical"))
                .andExpect(jsonPath("$.usage.inputBytes")
                        .value(100))
                .andExpect(jsonPath("$.usage.chunks")
                        .value(2))
                .andExpect(jsonPath("$.usage.cacheHit")
                        .value(false))
                .andExpect(jsonPath("$.failureMessage")
                        .doesNotExist());
    }

    @Test
    void omitsResultFieldsWhileJobIsQueued()
            throws Exception {

        when(jobService.get("job-queued"))
                .thenReturn(new ReviewJobSnapshot(
                        "job-queued",
                        JobStatus.QUEUED,
                        List.of(),
                        null,
                        null
                ));

        mockMvc.perform(get("/v1/reviews/job-queued"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("queued"))
                .andExpect(jsonPath("$.findings")
                        .doesNotExist())
                .andExpect(jsonPath("$.usage")
                        .doesNotExist())
                .andExpect(jsonPath("$.failureMessage")
                        .doesNotExist());
    }

    @Test
    void returnsClearFailureForFailedJob()
            throws Exception {

        when(jobService.get("job-failed"))
                .thenReturn(new ReviewJobSnapshot(
                        "job-failed",
                        JobStatus.FAILED,
                        List.of(),
                        null,
                        "Review processing failed: LLM unavailable"
                ));

        mockMvc.perform(get("/v1/reviews/job-failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("failed"))
                .andExpect(jsonPath("$.failureMessage")
                        .value(
                                "Review processing failed: "
                                        + "LLM unavailable"
                        ));
    }

    @Test
    void returnsInvalidDiffEnvelope()
            throws Exception {

        when(diffParser.parse(""))
                .thenThrow(new InvalidDiffException(
                        "Diff must not be empty"
                ));

        String body = objectMapper.writeValueAsString(
                Map.of("diff", "")
        );

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status()
                        .isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code")
                        .value("invalid_diff"))
                .andExpect(jsonPath("$.error.message")
                        .value("Diff must not be empty"));
    }

    @Test
    void returnsInvalidJsonEnvelope()
            throws Exception {

        mockMvc.perform(post("/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diff\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("invalid_json"));
    }

    @Test
    void returnsNotFoundEnvelopeForUnknownJob()
            throws Exception {

        when(jobService.get("missing"))
                .thenThrow(
                        new ReviewJobNotFoundException(
                                "missing"
                        )
                );

        mockMvc.perform(get("/v1/reviews/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code")
                        .value("not_found"))
                .andExpect(jsonPath("$.error.message")
                        .value(
                                "Review job not found: missing"
                        ));
    }

    private String validDiff() {
        return """
                --- a/src/app.js
                +++ b/src/app.js
                @@ -1,1 +1,1 @@
                -old
                +eval(input);
                """;
    }

    private ParsedDiff parsedDiff(String rawDiff) {
        return new ParsedDiff(List.of(
                new DiffFile("src/app.js",rawDiff,List.of())
        ));
    }
}