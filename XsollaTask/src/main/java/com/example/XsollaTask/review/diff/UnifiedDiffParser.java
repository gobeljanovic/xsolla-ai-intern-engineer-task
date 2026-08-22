package com.example.XsollaTask.review.diff;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public final class UnifiedDiffParser {

    private static final Pattern HUNK_HEADER = Pattern.compile(
            "^@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@.*$"
    );

    public ParsedDiff parse(String diff) {
        if (diff == null || diff.isBlank()) {
            throw new InvalidDiffException(
                    "Diff must not be empty"
            );
        }

        List<RawLine> lines = splitPreservingLineEndings(diff);
        List<Integer> fileStarts = findFileStarts(lines);

        if (fileStarts.isEmpty()) {
            throw new InvalidDiffException(
                    "No unified-diff file headers found"
            );
        }

        List<DiffFile> files = new ArrayList<>();

        for (int index = 0; index < fileStarts.size(); index++) {
            int start = fileStarts.get(index);
            int end = index + 1 < fileStarts.size()
                    ? fileStarts.get(index + 1)
                    : lines.size();

            files.add(parseFile(lines, start, end));
        }

        return new ParsedDiff(files);
    }

    private DiffFile parseFile(
            List<RawLine> lines,
            int start,
            int end
    ) {
        int oldHeaderIndex = findFileHeader(lines, start, end);

        if (oldHeaderIndex < 0) {
            throw new InvalidDiffException(
                    "File is missing --- and +++ headers"
            );
        }

        String oldPath = extractPath(
                lines.get(oldHeaderIndex).content(),
                "--- "
        );

        String newPath = extractPath(
                lines.get(oldHeaderIndex + 1).content(),
                "+++ "
        );

        String findingPath = "/dev/null".equals(newPath)
                ? normalizePath(oldPath)
                : normalizePath(newPath);

        List<NewFileLine> newFileLines  = new ArrayList<>();
        boolean foundHunk = false;

        int lineIndex = oldHeaderIndex + 2;

        while (lineIndex < end) {
            String content = lines.get(lineIndex).content();

            if (content.isBlank()) {
                lineIndex++;
                continue;
            }

            if (!content.startsWith("@@ ")) {
                throw new InvalidDiffException(
                        "Unexpected content outside a hunk: " + content
                );
            }

            HunkHeader hunk = parseHunkHeader(content);
            foundHunk = true;
            lineIndex++;

            int oldConsumed = 0;
            int newConsumed = 0;
            int newLineNumber = hunk.newStart();

            while (lineIndex < end) {
                String hunkLine = lines.get(lineIndex).content();

                if (hunkLine.startsWith("@@ ")) {
                    break;
                }

                if (hunkLine.equals("\\ No newline at end of file")) {
                    lineIndex++;
                    continue;
                }

                if (oldConsumed == hunk.oldCount()
                        && newConsumed == hunk.newCount()) {
                    throw new InvalidDiffException(
                            "Unexpected content after completed hunk"
                    );
                }

                if (hunkLine.isEmpty()) {
                    throw new InvalidDiffException(
                            "Hunk line is missing its prefix"
                    );
                }

                char prefix = hunkLine.charAt(0);

                switch (prefix) {
                    case ' ' -> {
                        newFileLines.add(new NewFileLine(
                                newLineNumber,
                                hunkLine.substring(1),
                                false
                        ));

                        oldConsumed++;
                        newConsumed++;
                        newLineNumber++;
                    }

                    case '-' -> oldConsumed++;

                    case '+' -> {
                        if (newLineNumber < 1) {
                            throw new InvalidDiffException(
                                    "Added line has an invalid line number"
                            );
                        }

                        newFileLines.add(new NewFileLine(
                                newLineNumber,
                                hunkLine.substring(1),
                                true
                        ));

                        newConsumed++;
                        newLineNumber++;
                    }

                    default -> throw new InvalidDiffException(
                            "Invalid hunk line prefix: " + prefix
                    );
                }

                if (oldConsumed > hunk.oldCount()
                        || newConsumed > hunk.newCount()) {
                    throw new InvalidDiffException(
                            "Hunk contains more lines than declared"
                    );
                }

                lineIndex++;
            }

            if (oldConsumed != hunk.oldCount()
                    || newConsumed != hunk.newCount()) {
                throw new InvalidDiffException(
                        "Hunk line counts do not match its header"
                );
            }
        }

        if (!foundHunk) {
            throw new InvalidDiffException(
                    "File diff does not contain a hunk"
            );
        }

        StringBuilder rawDiff = new StringBuilder();

        for (int index = start; index < end; index++) {
            rawDiff.append(lines.get(index).raw());
        }

        return new DiffFile(
                findingPath,
                rawDiff.toString(),
                newFileLines
        );
    }

    private int findFileHeader(
            List<RawLine> lines,
            int start,
            int end
    ) {
        for (int index = start; index + 1 < end; index++) {
            boolean oldHeader =
                    lines.get(index).content().startsWith("--- ");

            boolean newHeader =
                    lines.get(index + 1).content().startsWith("+++ ");

            if (oldHeader && newHeader) {
                return index;
            }
        }

        return -1;
    }

    private List<Integer> findFileStarts(List<RawLine> lines) {
        List<Integer> gitStarts = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).content().startsWith("diff --git ")) {
                gitStarts.add(index);
            }
        }

        if (!gitStarts.isEmpty()) {
            return gitStarts;
        }

        List<Integer> traditionalStarts = new ArrayList<>();

        for (int index = 0; index + 1 < lines.size(); index++) {
            boolean oldHeader =
                    lines.get(index).content().startsWith("--- ");

            boolean newHeader =
                    lines.get(index + 1).content().startsWith("+++ ");

            if (oldHeader && newHeader) {
                traditionalStarts.add(index);
            }
        }

        return traditionalStarts;
    }

    private HunkHeader parseHunkHeader(String line) {
        Matcher matcher = HUNK_HEADER.matcher(line);

        if (!matcher.matches()) {
            throw new InvalidDiffException(
                    "Invalid hunk header: " + line
            );
        }

        try {
            int oldStart = Integer.parseInt(matcher.group(1));
            int oldCount = matcher.group(2) == null
                    ? 1
                    : Integer.parseInt(matcher.group(2));

            int newStart = Integer.parseInt(matcher.group(3));
            int newCount = matcher.group(4) == null
                    ? 1
                    : Integer.parseInt(matcher.group(4));

            return new HunkHeader(
                    oldStart,
                    oldCount,
                    newStart,
                    newCount
            );
        } catch (NumberFormatException exception) {
            throw new InvalidDiffException(
                    "Hunk header contains an invalid number"
            );
        }
    }

    private String extractPath(
            String header,
            String prefix
    ) {
        String path = header.substring(prefix.length());

        int timestampSeparator = path.indexOf('\t');

        if (timestampSeparator >= 0) {
            path = path.substring(0, timestampSeparator);
        }

        path = path.trim();

        if (path.isEmpty()) {
            throw new InvalidDiffException(
                    "File header contains an empty path"
            );
        }

        return path;
    }

    private String normalizePath(String path) {
        if (path.startsWith("a/") || path.startsWith("b/")) {
            return path.substring(2);
        }

        return path;
    }

    private List<RawLine> splitPreservingLineEndings(String text) {
        List<RawLine> lines = new ArrayList<>();

        int start = 0;

        while (start < text.length()) {
            int newline = text.indexOf('\n', start);
            int end = newline >= 0
                    ? newline + 1
                    : text.length();

            String raw = text.substring(start, end);
            String content = raw;

            if (content.endsWith("\n")) {
                content = content.substring(
                        0,
                        content.length() - 1
                );
            }

            if (content.endsWith("\r")) {
                content = content.substring(
                        0,
                        content.length() - 1
                );
            }

            lines.add(new RawLine(raw, content));
            start = end;
        }

        return lines;
    }

    private record RawLine(
            String raw,
            String content
    ) {
    }

    private record HunkHeader(
            int oldStart,
            int oldCount,
            int newStart,
            int newCount
    ) {
    }
}