package com.example.XsollaTask.review.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FindingTest {

    @Test
    void createsContractFindingId() {
        Finding finding = Finding.create(
                "MOCK-003",
                "src/db.ts",
                41,
                Severity.HIGH,
                Category.SECURITY,
                "SQL string concatenation",
                "const query = \"SELECT\" + input;"
        );

        assertThat(finding.id())
                .isEqualTo("MOCK-003:src/db.ts:41");
    }

    @Test
    void sortsByPathThenLineThenRuleId() {
        Finding third = Finding.create(
                "MOCK-001",
                "src/z.ts",
                1,
                Severity.CRITICAL,
                Category.SECURITY,
                "eval usage",
                "eval(input);"
        );

        Finding second = Finding.create(
                "MOCK-008",
                "src/a.ts",
                10,
                Severity.LOW,
                Category.STYLE,
                "unresolved marker",
                "// TODO"
        );

        Finding first = Finding.create(
                "MOCK-001",
                "src/a.ts",
                10,
                Severity.CRITICAL,
                Category.SECURITY,
                "eval usage",
                "eval(input);"
        );

        List<Finding> findings =
                new ArrayList<>(List.of(third, second, first));

        findings.sort(Finding.REQUIRED_ORDER);

        assertThat(findings)
                .containsExactly(first, second, third);
    }
}