package com.example.XsollaTask.health;

import com.example.XsollaTask.config.LimitsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public final class SpecController {

    private static final List<String> PROVIDERS =
            List.of("mock", "llm");

    private final LimitsProperties limitsProperties;

    @GetMapping("/spec")
    public SpecResponse spec() {
        SpecLimitsResponse limits = new SpecLimitsResponse(
                limitsProperties.maxPayloadBytes(),
                limitsProperties.chunkBytes(),
                limitsProperties.maxConcurrentJobs(),
                limitsProperties.rateLimitPerMinute()
        );

        return new SpecResponse(
                "1.0",
                PROVIDERS,
                limits
        );
    }
}