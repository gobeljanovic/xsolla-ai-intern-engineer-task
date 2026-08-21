package com.example.XsollaTask.review.diff;

import java.util.Objects;

public record AddedLine(
        int newLineNumber,
        String content
) {
    public AddedLine {
        if (newLineNumber < 1) {
            throw new IllegalArgumentException(
                    "New-file line number must be positive"
            );
        }

        Objects.requireNonNull(content);
    }
}