package com.example.XsollaTask.review.provider;

import com.example.XsollaTask.review.diff.ParsedDiff;
import com.example.XsollaTask.review.domain.Finding;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class LlmReviewProvider
        implements ReviewProvider {

    @Override
    public List<Finding> review(ParsedDiff diff) {
        throw new IllegalStateException(
                "LLM provider is not configured"
        );
    }
}