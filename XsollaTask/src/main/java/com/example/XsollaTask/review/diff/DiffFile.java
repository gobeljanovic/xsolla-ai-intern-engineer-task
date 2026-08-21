package com.example.XsollaTask.review.diff;

import java.util.List;
import java.util.Objects;

public record DiffFile(
        String path,
        String rawDiff,
        List<AddedLine> addedLines
) {
    public DiffFile {
        Objects.requireNonNull(path);
        Objects.requireNonNull(rawDiff);
        Objects.requireNonNull(addedLines);

        addedLines = List.copyOf(addedLines);
    }
}