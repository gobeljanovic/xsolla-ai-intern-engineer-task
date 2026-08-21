package com.example.XsollaTask.review.diff;

import java.util.List;
import java.util.Objects;

public record ParsedDiff(List<DiffFile> files) {
    public ParsedDiff {
        Objects.requireNonNull(files);
        files = List.copyOf(files);

        if (files.isEmpty()) {
            throw new IllegalArgumentException(
                    "A parsed diff must contain at least one file"
            );
        }
    }
}