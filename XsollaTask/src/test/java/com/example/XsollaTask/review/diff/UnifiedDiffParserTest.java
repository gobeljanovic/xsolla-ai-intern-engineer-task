package com.example.XsollaTask.review.diff;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedDiffParserTest {

    private final UnifiedDiffParser parser =
            new UnifiedDiffParser();

    @Test
    void tracksAddedLineNumbersInTheNewFile() {
        String diff = """
                diff --git a/src/app.js b/src/app.js
                --- a/src/app.js
                +++ b/src/app.js
                @@ -10,3 +10,4 @@
                 const value = input;
                -oldCall();
                +eval(input);
                +console.log(input);
                 return value;
                """;

        ParsedDiff parsed = parser.parse(diff);

        assertThat(parsed.files()).hasSize(1);

        DiffFile file = parsed.files().get(0);

        assertThat(file.path()).isEqualTo("src/app.js");

        assertThat(file.addedLines())
                .containsExactly(
                        new AddedLine(11, "eval(input);"),
                        new AddedLine(12, "console.log(input);")
                );
    }

    @Test
    void rejectsTextThatIsNotAUnifiedDiff() {
        String input = "console.log('not a diff');";

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> parser.parse(input)
                )
                .isInstanceOf(InvalidDiffException.class);
    }
}