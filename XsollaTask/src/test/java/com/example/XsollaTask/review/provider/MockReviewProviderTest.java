package com.example.XsollaTask.review.provider;

import com.example.XsollaTask.review.diff.AddedLine;
import com.example.XsollaTask.review.diff.DiffFile;
import com.example.XsollaTask.review.diff.ParsedDiff;
import com.example.XsollaTask.review.domain.Category;
import com.example.XsollaTask.review.domain.Finding;
import com.example.XsollaTask.review.domain.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockReviewProviderTest {

    private final MockReviewProvider provider =
            new MockReviewProvider();

    @Test
    void detectsEvalOnAddedLine() {
        ParsedDiff diff = new ParsedDiff(List.of(
                new DiffFile(
                        "src/app.js",
                        "",
                        List.of(
                                new AddedLine(
                                        17,
                                        "const result = eval(input);"
                                )
                        )
                )
        ));

        List<Finding> findings = provider.review(diff);

        Finding expected = Finding.create(
                "MOCK-001",
                "src/app.js",
                17,
                Severity.CRITICAL,
                Category.SECURITY,
                "eval usage",
                "const result = eval(input);"
        );

        assertThat(findings).containsExactly(expected);
    }

    @Test
    void doesNotReportLineWithoutEval() {
        ParsedDiff diff = diffWithAddedLine(
                17,
                "const result = parse(input);"
        );

        List<Finding> findings = provider.review(diff);

        assertThat(findings).isEmpty();
    }

    @Test
    void detectsHardcodedCredential() {
        ParsedDiff diff = diffWithAddedLine(
                23,
                "const API_KEY = \"abcdefghijklmnop\";"
        );

        List<Finding> findings = provider.review(diff);

        Finding expected = Finding.create(
                "MOCK-002",
                "src/app.js",
                23,
                Severity.CRITICAL,
                Category.SECURITY,
                "hardcoded credential",
                "const API_KEY = \"abcdefghijklmnop\";"
        );

        assertThat(findings).containsExactly(expected);
    }

    @Test
    void ignoresCredentialShorterThanSixteenCharacters() {
        ParsedDiff diff = diffWithAddedLine(
                23,
                "const token = \"too-short\";"
        );

        assertThat(provider.review(diff)).isEmpty();
    }

    private ParsedDiff diffWithAddedLine(
            int lineNumber,
            String content
    ) {
        return new ParsedDiff(List.of(
                new DiffFile(
                        "src/app.js",
                        "",
                        List.of(new AddedLine(lineNumber, content))
                )
        ));
    }
}