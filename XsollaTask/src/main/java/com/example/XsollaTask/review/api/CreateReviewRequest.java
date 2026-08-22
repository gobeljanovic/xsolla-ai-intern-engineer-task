package com.example.XsollaTask.review.api;

import com.example.XsollaTask.review.provider.ProviderType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateReviewRequest(
        String diff,
        ReviewOptions options
) {
    public ProviderType effectiveProvider() {
        return options == null
                ? ProviderType.MOCK
                : options.effectiveProvider();
    }

    public int effectiveMaxFindings() {
        return options == null
                ? ReviewOptions.DEFAULT_MAX_FINDINGS
                : options.effectiveMaxFindings();
    }
}