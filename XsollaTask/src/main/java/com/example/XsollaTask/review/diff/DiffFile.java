package com.example.XsollaTask.review.diff;

import java.util.List;
import java.util.Objects;

public record DiffFile(
        String path,
        String rawDiff,
        List<NewFileLine> newFileLines
) {
    public DiffFile {
        Objects.requireNonNull(path);
        Objects.requireNonNull(rawDiff);
        Objects.requireNonNull(newFileLines);

        newFileLines = List.copyOf(newFileLines);
    }

    public List<AddedLine> addedLines() {
        return newFileLines.stream()
                .filter(NewFileLine::added)
                .map(line -> new AddedLine(
                        line.newLineNumber(),
                        line.content()
                ))
                .toList();
    }
}