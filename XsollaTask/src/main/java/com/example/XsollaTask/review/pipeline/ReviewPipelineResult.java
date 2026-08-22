package com.example.XsollaTask.review.pipeline;

import com.example.XsollaTask.review.domain.Finding;

import java.util.List;
import java.util.Objects;

public record ReviewPipelineResult(
        List<Finding> findings,
        int chunks
) {
    public ReviewPipelineResult {
        Objects.requireNonNull(findings);
        findings = List.copyOf(findings);

        if (chunks < 1) {
            throw new IllegalArgumentException(
                    "Chunk count must be positive"
            );
        }
    }
}