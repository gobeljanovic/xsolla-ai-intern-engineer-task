package com.example.XsollaTask.review.pipeline;

import com.example.XsollaTask.review.diff.ParsedDiff;
import com.example.XsollaTask.review.domain.Finding;
import com.example.XsollaTask.review.provider.ReviewProvider;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public final class ReviewPipeline {

    private final DiffChunker diffChunker;

    public ReviewPipeline(DiffChunker diffChunker) {
        this.diffChunker =
                Objects.requireNonNull(diffChunker);
    }

    public ReviewPipelineResult execute(
            ReviewProvider provider,
            ParsedDiff diff,
            int maxFindings
    ) {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(diff);

        if (maxFindings < 0) {
            throw new IllegalArgumentException(
                    "maxFindings must not be negative"
            );
        }

        List<ParsedDiff> chunks =
                diffChunker.chunk(diff);

        List<Finding> discovered =
                new ArrayList<>();

        for (ParsedDiff chunk : chunks) {
            discovered.addAll(
                    provider.review(chunk)
            );
        }

        Map<String, Finding> uniqueById =
                new HashMap<>();

        for (Finding finding : discovered) {
            uniqueById.putIfAbsent(
                    finding.id(),
                    finding
            );
        }

        List<Finding> processed = uniqueById.values()
                .stream()
                .sorted(Finding.REQUIRED_ORDER)
                .limit(maxFindings)
                .toList();

        return new ReviewPipelineResult(
                processed,
                chunks.size()
        );
    }
}