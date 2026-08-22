package com.example.XsollaTask.review.provider;

import com.example.XsollaTask.review.diff.AddedLine;
import com.example.XsollaTask.review.diff.DiffFile;
import com.example.XsollaTask.review.diff.ParsedDiff;
import com.example.XsollaTask.review.domain.Category;
import com.example.XsollaTask.review.domain.Finding;
import com.example.XsollaTask.review.domain.Severity;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.regex.Matcher;
import com.example.XsollaTask.review.diff.NewFileLine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public final class MockReviewProvider implements ReviewProvider {

    private static final Pattern LOOSE_NULL_COMPARISON_PATTERN =
            Pattern.compile(
                    "(?<![=!])(?:==|!=) null"
            );
    private static final Pattern HARDCODED_CREDENTIAL_PATTERN =
            Pattern.compile(
                    "(api[_-]?key|secret|token)\\s*[:=]\\s*['\"][A-Za-z0-9_-]{16,}['\"]",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern STRING_LITERAL_PATTERN =
            Pattern.compile(
                    "(['\"])(?:\\\\.|(?!\\1).)*\\1"
            );

    private static final Pattern SQL_KEYWORD_PATTERN =
            Pattern.compile(
                    "\\b(?:SELECT|INSERT|UPDATE|DELETE)\\b"
            );

    private static final Pattern CATCH_OPEN_PATTERN =
            Pattern.compile(
                    "\\bcatch\\s*(?:\\([^)]*\\))?\\s*\\{"
            );

    @Override
    public List<Finding> review(ParsedDiff diff) {
        List<Finding> findings = new ArrayList<>();

        for (DiffFile file : diff.files()) {
            for (AddedLine addedLine : file.addedLines()) {
                detectEval(file.path(), addedLine, findings);
                detectHardcodedCredential(file.path(), addedLine, findings);
                detectLooseNullComparison(file.path(), addedLine, findings);
                detectJsonDeepClone(file.path(), addedLine, findings);
                detectConsoleLog(file.path(), addedLine, findings);
                detectUnresolvedMarker(file.path(), addedLine, findings);
                detectPromptInjection(file.path(), addedLine, findings);
                detectSqlStringConcatenation(file.path(), addedLine, findings);
            }
            detectEmptyCatchBlocks(file, findings);
        }

        return List.copyOf(findings);
    }

    private void detectEval(
            String path,
            AddedLine addedLine,
            List<Finding> findings
    ) {
        if (addedLine.content().contains("eval(")) {
            findings.add(Finding.create(
                    "MOCK-001",
                    path,
                    addedLine.newLineNumber(),
                    Severity.CRITICAL,
                    Category.SECURITY,
                    "eval usage",
                    addedLine.content()
            ));
        }
    }

    private void detectHardcodedCredential(
            String path,
            AddedLine addedLine,
            List<Finding> findings
    ) {
        if (HARDCODED_CREDENTIAL_PATTERN
                .matcher(addedLine.content())
                .find()) {

            findings.add(Finding.create(
                    "MOCK-002",
                    path,
                    addedLine.newLineNumber(),
                    Severity.CRITICAL,
                    Category.SECURITY,
                    "hardcoded credential",
                    addedLine.content()
            ));
        }
    }
    private void detectLooseNullComparison(
            String path,
            AddedLine addedLine,
            List<Finding> findings
    ) {
        if (LOOSE_NULL_COMPARISON_PATTERN
                .matcher(addedLine.content())
                .find()) {

            findings.add(Finding.create(
                    "MOCK-005",
                    path,
                    addedLine.newLineNumber(),
                    Severity.MEDIUM,
                    Category.CORRECTNESS,
                    "loose null comparison",
                    addedLine.content()
            ));
        }
    }

    private void detectJsonDeepClone(
            String path,
            AddedLine addedLine,
            List<Finding> findings
    ) {
        if (addedLine.content().contains(
                "JSON.parse(JSON.stringify("
        )) {
            findings.add(Finding.create(
                    "MOCK-006",
                    path,
                    addedLine.newLineNumber(),
                    Severity.MEDIUM,
                    Category.PERFORMANCE,
                    "deep-clone via JSON",
                    addedLine.content()
            ));
        }
    }

    private void detectConsoleLog(
            String path,
            AddedLine addedLine,
            List<Finding> findings
    ) {
        if (addedLine.content().contains("console.log(")) {
            findings.add(Finding.create(
                    "MOCK-007",
                    path,
                    addedLine.newLineNumber(),
                    Severity.LOW,
                    Category.STYLE,
                    "console.log left in",
                    addedLine.content()
            ));
        }
    }

    private void detectUnresolvedMarker(
            String path,
            AddedLine addedLine,
            List<Finding> findings
    ) {
        String content = addedLine.content();

        if (content.contains("TODO") || content.contains("FIXME")) {
            findings.add(Finding.create(
                    "MOCK-008",
                    path,
                    addedLine.newLineNumber(),
                    Severity.LOW,
                    Category.STYLE,
                    "unresolved marker",
                    content
            ));
        }
    }
    private void detectPromptInjection(
            String path,
            AddedLine addedLine,
            List<Finding> findings
    ) {
        String normalized = addedLine.content()
                .toLowerCase(Locale.ROOT);

        boolean detected =
                normalized.contains("ignore previous instructions")
                        || normalized.contains("disregard all prior")
                        || normalized.contains("you are now");

        if (detected) {
            findings.add(Finding.create(
                    "MOCK-INJ",
                    path,
                    addedLine.newLineNumber(),
                    Severity.CRITICAL,
                    Category.SECURITY,
                    "prompt-injection content",
                    addedLine.content()
            ));
        }
    }
    private void detectSqlStringConcatenation(
            String path,
            AddedLine addedLine,
            List<Finding> findings
    ) {
        String content = addedLine.content();
        Matcher stringMatcher =
                STRING_LITERAL_PATTERN.matcher(content);

        while (stringMatcher.find()) {
            String stringLiteral = stringMatcher.group();

            boolean containsSqlKeyword =
                    SQL_KEYWORD_PATTERN
                            .matcher(stringLiteral)
                            .find();

            boolean concatenated =
                    isConcatenated(content, stringMatcher);

            if (containsSqlKeyword && concatenated) {
                findings.add(Finding.create(
                        "MOCK-003",
                        path,
                        addedLine.newLineNumber(),
                        Severity.HIGH,
                        Category.SECURITY,
                        "SQL string concatenation",
                        content
                ));

                return;
            }
        }
    }

    private void detectEmptyCatchBlocks(
            DiffFile file,
            List<Finding> findings
    ) {
        List<NewFileLine> lines = file.newFileLines();

        for (int index = 0; index < lines.size(); index++) {
            NewFileLine catchLine = lines.get(index);

            // The contract applies rules only to added lines.
            if (!catchLine.added()) {
                continue;
            }

            Matcher matcher =
                    CATCH_OPEN_PATTERN.matcher(catchLine.content());

            if (!matcher.find()) {
                continue;
            }

            if (isEmptyCatchBlock(lines, index, matcher.end())) {
                findings.add(Finding.create(
                        "MOCK-004",
                        file.path(),
                        catchLine.newLineNumber(),
                        Severity.HIGH,
                        Category.CORRECTNESS,
                        "swallowed exception",
                        catchLine.content()
                ));
            }
        }
    }

    private boolean isConcatenated(
            String completeLine,
            Matcher stringMatcher
    ) {
        String beforeString = completeLine
                .substring(0, stringMatcher.start())
                .stripTrailing();

        String afterString = completeLine
                .substring(stringMatcher.end())
                .stripLeading();

        return beforeString.endsWith("+")
                || afterString.startsWith("+");
    }

    private boolean isEmptyCatchBlock(
            List<NewFileLine> lines,
            int catchLineIndex,
            int openingBraceEnd
    ) {
        NewFileLine catchLine = lines.get(catchLineIndex);

        String afterOpeningBrace = catchLine.content()
                .substring(openingBraceEnd)
                .stripLeading();

        // Handles: catch (error) {}
        if (!afterOpeningBrace.isEmpty()) {
            return afterOpeningBrace.startsWith("}");
        }

        int expectedLineNumber =
                catchLine.newLineNumber() + 1;

        for (int index = catchLineIndex + 1; index < lines.size(); index++)
        {
            NewFileLine nextLine = lines.get(index);

            // Do not cross a gap between diff hunks.
            if (nextLine.newLineNumber() != expectedLineNumber) {
                return false;
            }

            expectedLineNumber++;

            String content = nextLine.content().stripLeading();

            if (content.isEmpty()) {
                continue;
            }

            // The first nonblank content closes the catch.
            return content.startsWith("}");
        }

        return false;
    }


}