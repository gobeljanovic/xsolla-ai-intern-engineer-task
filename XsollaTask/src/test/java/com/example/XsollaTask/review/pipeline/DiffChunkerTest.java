package com.example.XsollaTask.review.pipeline;

import com.example.XsollaTask.config.LimitsProperties;
import com.example.XsollaTask.review.diff.DiffFile;
import com.example.XsollaTask.review.diff.ParsedDiff;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiffChunkerTest {

    @Test
    void keepsFilesTogetherWhenTheirTotalFits() {
        DiffFile first = file("a.js", "1234");
        DiffFile second = file("b.js", "123456");

        List<ParsedDiff> chunks = chunker(10).chunk(
                parsedDiff(first, second)
        );

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).files())
                .containsExactly(first, second);
    }

    @Test
    void splitsOnlyAtFileBoundary() {
        DiffFile first = file("a.js", "123456");
        DiffFile second = file("b.js", "abcdef");

        List<ParsedDiff> chunks = chunker(10).chunk(
                parsedDiff(first, second)
        );

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).files())
                .containsExactly(first);
        assertThat(chunks.get(1).files())
                .containsExactly(second);
    }

    @Test
    void putsOversizedFileInItsOwnChunk() {
        DiffFile oversized =
                file("large.js", "12345678901");

        DiffFile small =
                file("small.js", "12");

        List<ParsedDiff> chunks = chunker(10).chunk(
                parsedDiff(oversized, small)
        );

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).files())
                .containsExactly(oversized);
        assertThat(chunks.get(1).files())
                .containsExactly(small);
    }

    @Test
    void measuresUtf8BytesInsteadOfCharacters() {
        DiffFile first = file("a.js", "éé");
        DiffFile second = file("b.js", "éé");

        List<ParsedDiff> chunks = chunker(6).chunk(
                parsedDiff(first, second)
        );

        // Each file is 4 UTF-8 bytes, so 4 + 4 exceeds 6.
        assertThat(chunks).hasSize(2);
    }

    private DiffChunker chunker(int chunkBytes) {
        LimitsProperties limits = new LimitsProperties(
                1_048_576,
                chunkBytes,
                4,
                30
        );

        return new DiffChunker(limits);
    }

    private ParsedDiff parsedDiff(DiffFile... files) {
        return new ParsedDiff(List.of(files));
    }

    private DiffFile file(String path, String rawDiff) {
        return new DiffFile(
                path,
                rawDiff,
                List.of()
        );
    }
}