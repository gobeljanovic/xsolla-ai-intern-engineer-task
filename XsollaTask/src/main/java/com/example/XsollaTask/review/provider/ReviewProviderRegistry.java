package com.example.XsollaTask.review.provider;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public final class ReviewProviderRegistry {

    private final Map<ProviderType, ReviewProvider> providers;

    public ReviewProviderRegistry(
            MockReviewProvider mockProvider,
            LlmReviewProvider llmProvider
    ) {
        providers = Map.of(
                ProviderType.MOCK,
                Objects.requireNonNull(mockProvider),
                ProviderType.LLM,
                Objects.requireNonNull(llmProvider)
        );
    }

    public ReviewProvider resolve(ProviderType type) {
        Objects.requireNonNull(type);

        ReviewProvider provider = providers.get(type);

        if (provider == null) {
            throw new IllegalArgumentException(
                    "Unsupported provider: " + type
            );
        }

        return provider;
    }
}