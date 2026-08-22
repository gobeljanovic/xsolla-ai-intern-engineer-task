package com.example.XsollaTask.review.diff;

import java.util.Objects;

public record NewFileLine(
        int newLineNumber,
        String content,
        boolean added
) {
    public NewFileLine {
        if (newLineNumber < 1) {
            throw new IllegalArgumentException(
                    "New-file line number must be positive"
            );
        }

        Objects.requireNonNull(content);
    }
}