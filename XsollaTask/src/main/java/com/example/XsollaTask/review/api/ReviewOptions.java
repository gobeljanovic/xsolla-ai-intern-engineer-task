package com.example.XsollaTask.review.api;

import com.example.XsollaTask.review.provider.ProviderType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewOptions(
        ProviderType provider,
        Integer maxFindings
) {
    public static final int DEFAULT_MAX_FINDINGS = 100;

    public ReviewOptions {
        if (maxFindings != null && maxFindings < 0) {
            throw new IllegalArgumentException(
                    "maxFindings must not be negative"
            );
        }
    }

    public ProviderType effectiveProvider() {
        return provider == null
                ? ProviderType.MOCK
                : provider;
    }

    public int effectiveMaxFindings() {
        return maxFindings == null
                ? DEFAULT_MAX_FINDINGS
                : maxFindings;
    }
}