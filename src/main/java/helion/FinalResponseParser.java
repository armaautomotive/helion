package helion;

import java.util.ArrayList;
import java.util.List;

public final class FinalResponseParser {
    private FinalResponseParser() {
    }

    public static FinalResponse parse(String raw) {
        String text = raw == null ? "" : raw.trim();
        String status = valueAfter(text, "STATUS:");
        String title = valueAfter(text, "TITLE:");
        String summary = blockBetween(text, "SUMMARY:", "DETAILS:", "NEXT_STEPS:", "SOURCES:", "STATUS:");
        String details = blockBetween(text, "DETAILS:", "NEXT_STEPS:", "SOURCES:", "STATUS:");
        List<String> nextSteps = linesAfter(text, "NEXT_STEPS:", "SOURCES:", "STATUS:");
        List<String> sources = linesAfter(text, "SOURCES:", "STATUS:");

        if (title.isBlank() && summary.isBlank() && details.isBlank() && nextSteps.isEmpty()) {
            return new FinalResponse("", "", text, "", List.of(), List.of());
        }
        return new FinalResponse(status, title, summary, details, nextSteps, sources);
    }

    private static String valueAfter(String text, String label) {
        int start = text.indexOf(label);
        if (start < 0) {
            return "";
        }
        int lineStart = start + label.length();
        int lineEnd = text.indexOf('\n', lineStart);
        if (lineEnd < 0) {
            lineEnd = text.length();
        }
        return text.substring(lineStart, lineEnd).trim();
    }

    private static String blockBetween(String text, String startLabel, String... endLabels) {
        int start = text.indexOf(startLabel);
        if (start < 0) {
            return "";
        }
        int contentStart = start + startLabel.length();
        int end = text.length();
        for (String endLabel : endLabels) {
            int candidate = text.indexOf(endLabel, contentStart);
            if (candidate >= 0 && candidate < end) {
                end = candidate;
            }
        }
        return text.substring(contentStart, end).trim();
    }

    private static List<String> linesAfter(String text, String startLabel, String... endLabels) {
        String block = blockBetween(text, startLabel, endLabels);
        if (block.isBlank()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (String line : splitLines(block)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("-")) {
                trimmed = trimmed.substring(1).trim();
            } else {
                trimmed = stripNumberedPrefix(trimmed);
            }
            lines.add(trimmed);
        }
        return lines;
    }

    private static List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r') {
                continue;
            }
            if (c == '\n') {
                lines.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        lines.add(current.toString());
        return lines;
    }

    private static String stripNumberedPrefix(String value) {
        int i = 0;
        while (i < value.length() && Character.isDigit(value.charAt(i))) {
            i++;
        }
        if (i == 0 || i >= value.length() || value.charAt(i) != '.') {
            return value;
        }
        int next = i + 1;
        while (next < value.length() && Character.isWhitespace(value.charAt(next))) {
            next++;
        }
        return next < value.length() ? value.substring(next) : value;
    }
}
