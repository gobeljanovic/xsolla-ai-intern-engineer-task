package com.example.XsollaTask.review.domain;

import java.util.Comparator;
import java.util.Objects;

public record Finding(
        String id,
        String ruleId,
        String path,
        int line,
        Severity severity,
        Category category,
        String title,
        String evidence
) {
    public static final Comparator<Finding> REQUIRED_ORDER =
            Comparator.comparing(Finding::path)
                    .thenComparingInt(Finding::line)
                    .thenComparing(Finding::ruleId);

    public Finding {
        Objects.requireNonNull(id);
        Objects.requireNonNull(ruleId);
        Objects.requireNonNull(path);
        Objects.requireNonNull(severity);
        Objects.requireNonNull(category);
        Objects.requireNonNull(title);
        Objects.requireNonNull(evidence);

        if (line < 1) {
            throw new IllegalArgumentException(
                    "Finding line must be positive"
            );
        }
    }

    public static Finding create(
            String ruleId,
            String path,
            int line,
            Severity severity,
            Category category,
            String title,
            String evidence
    ) {
        String id = ruleId + ":" + path + ":" + line;

        return new Finding(
                id,
                ruleId,
                path,
                line,
                severity,
                category,
                title,
                evidence
        );
    }
}