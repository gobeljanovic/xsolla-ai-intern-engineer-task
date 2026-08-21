package com.example.XsollaTask.review.provider;

import com.example.XsollaTask.review.diff.AddedLine;
import com.example.XsollaTask.review.diff.DiffFile;
import com.example.XsollaTask.review.diff.ParsedDiff;
import com.example.XsollaTask.review.domain.Category;
import com.example.XsollaTask.review.domain.Finding;
import com.example.XsollaTask.review.domain.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public final class MockReviewProvider implements ReviewProvider {

    private static final Pattern HARDCODED_CREDENTIAL_PATTERN =
            Pattern.compile(
                    "(api[_-]?key|secret|token)\\s*[:=]\\s*['\"][A-Za-z0-9_-]{16,}['\"]",
                    Pattern.CASE_INSENSITIVE
            );

    @Override
    public List<Finding> review(ParsedDiff diff) {
        List<Finding> findings = new ArrayList<>();

        for (DiffFile file : diff.files()) {
            for (AddedLine addedLine : file.addedLines()) {
                detectEval(file.path(), addedLine, findings);
                detectHardcodedCredential(
                        file.path(),
                        addedLine,
                        findings
                );
            }
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
}