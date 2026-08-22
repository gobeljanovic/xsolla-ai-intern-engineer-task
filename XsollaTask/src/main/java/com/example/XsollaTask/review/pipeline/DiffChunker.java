package com.example.XsollaTask.review.pipeline;

import com.example.XsollaTask.config.LimitsProperties;
import com.example.XsollaTask.review.diff.DiffFile;
import com.example.XsollaTask.review.diff.ParsedDiff;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public final class DiffChunker {

    private final LimitsProperties limitsProperties;

    public DiffChunker(LimitsProperties limitsProperties) {
        this.limitsProperties =
                Objects.requireNonNull(limitsProperties);
    }

    public List<ParsedDiff> chunk(ParsedDiff diff) {
        Objects.requireNonNull(diff);

        int chunkBytes = limitsProperties.chunkBytes();

        List<ParsedDiff> chunks = new ArrayList<>();
        List<DiffFile> currentFiles = new ArrayList<>();
        int currentBytes = 0;

        for (DiffFile file : diff.files()) {
            int fileBytes = file.rawDiff()
                    .getBytes(StandardCharsets.UTF_8)
                    .length;

            boolean exceedsCurrentChunk =
                    !currentFiles.isEmpty()
                            && currentBytes + fileBytes > chunkBytes;

            if (exceedsCurrentChunk) {
                chunks.add(new ParsedDiff(currentFiles));
                currentFiles = new ArrayList<>();
                currentBytes = 0;
            }

            currentFiles.add(file);
            currentBytes += fileBytes;

            if (fileBytes > chunkBytes) {
                chunks.add(new ParsedDiff(currentFiles));
                currentFiles = new ArrayList<>();
                currentBytes = 0;
            }
        }

        if (!currentFiles.isEmpty()) {
            chunks.add(new ParsedDiff(currentFiles));
        }

        return List.copyOf(chunks);
    }
}