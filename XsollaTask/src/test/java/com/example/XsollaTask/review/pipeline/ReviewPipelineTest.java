package com.example.XsollaTask.review.pipeline;

import com.example.XsollaTask.review.diff.DiffFile;
import com.example.XsollaTask.review.diff.ParsedDiff;
import com.example.XsollaTask.review.domain.Category;
import com.example.XsollaTask.review.domain.Finding;
import com.example.XsollaTask.review.domain.Severity;
import com.example.XsollaTask.review.provider.ReviewProvider;
import org.junit.jupiter.api.Test;
import com.example.XsollaTask.config.LimitsProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewPipelineTest {

    private final ReviewPipeline pipeline =
            pipelineWithChunkBytes(65_536);

    @Test
    void ordersFindingsByPathLineAndRuleId() {
        Finding zPath = finding("MOCK-008", "z.js", 2);
        Finding laterLine = finding("MOCK-007", "a.js", 5);
        Finding earlierRule = finding("MOCK-006", "a.js", 5);
        Finding earlierLine = finding("MOCK-001", "a.js", 2);

        ReviewProvider provider = ignored -> List.of(
                zPath,
                laterLine,
                earlierRule,
                earlierLine
        );

        List<Finding> result = pipeline.execute(
                provider,
                emptyParsedDiff(),
                100
        ).findings();

        assertThat(result).containsExactly(
                earlierLine,
                earlierRule,
                laterLine,
                zPath
        );
    }

    @Test
    void deduplicatesFindingsById() {
        Finding finding =
                finding("MOCK-001", "src/app.js", 10);

        ReviewProvider provider = ignored -> List.of(
                finding,
                finding
        );

        List<Finding> result = pipeline.execute(
                provider,
                emptyParsedDiff(),
                100
        ).findings();

        assertThat(result).containsExactly(finding);
    }

    @Test
    void truncatesOnlyAfterOrdering() {
        Finding zPath =
                finding("MOCK-001", "z.js", 1);

        Finding second =
                finding("MOCK-001", "a.js", 2);

        Finding first =
                finding("MOCK-001", "a.js", 1);

        ReviewProvider provider = ignored -> List.of(
                zPath,
                second,
                first
        );

        List<Finding> result = pipeline.execute(
                provider,
                emptyParsedDiff(),
                2
        ).findings();

        assertThat(result).containsExactly(
                first,
                second
        );
    }

    private Finding finding(
            String ruleId,
            String path,
            int line
    ) {
        return Finding.create(
                ruleId,
                path,
                line,
                Severity.LOW,
                Category.STYLE,
                "test finding",
                "test evidence"
        );
    }

    private ParsedDiff emptyParsedDiff() {
        return new ParsedDiff(List.of(
                new DiffFile(
                        "src/app.js",
                        "",
                        List.of()
                )
        ));
    }

    private ReviewPipeline pipelineWithChunkBytes(
            int chunkBytes
    ) {
        LimitsProperties limits = new LimitsProperties(
                1_048_576,
                chunkBytes,
                4,
                30
        );

        return new ReviewPipeline(
                new DiffChunker(limits)
        );
    }
    @Test
    void chunkingDoesNotChangeFindingsAndReportsChunkCount() {
        DiffFile zFile = file("z.js", "123456");
        DiffFile aFile = file("a.js", "abcdef");

        ParsedDiff diff = new ParsedDiff(
                List.of(zFile, aFile)
        );

        ReviewProvider provider = chunk ->
                chunk.files()
                        .stream()
                        .map(file -> finding(
                                "MOCK-001",
                                file.path(),
                                1
                        ))
                        .toList();

        ReviewPipelineResult chunked =
                pipelineWithChunkBytes(10)
                        .execute(provider, diff, 100);

        ReviewPipelineResult unchunked =
                pipelineWithChunkBytes(20)
                        .execute(provider, diff, 100);

        assertThat(chunked.chunks()).isEqualTo(2);
        assertThat(unchunked.chunks()).isEqualTo(1);

        assertThat(chunked.findings())
                .containsExactlyElementsOf(unchunked.findings());
    }

    private DiffFile file(
            String path,
            String rawDiff
    ) {
        return new DiffFile(
                path,
                rawDiff,
                List.of()
        );
    }
}