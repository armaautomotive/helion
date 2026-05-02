package helion;

public final class ManagerActionParser {
    private ManagerActionParser() {
    }

    public static ManagerAction parse(String raw) {
        String text = raw == null ? "" : raw.trim();
        String action = valueAfter(text, "ACTION:");
        if ("WORKER".equalsIgnoreCase(action)) {
            String title = valueAfter(text, "TITLE:");
            String prompt = blockAfter(text, "PROMPT:");
            return new WorkerAction(title.isBlank() ? "subtask" : title, prompt);
        }
        if ("SEARCH".equalsIgnoreCase(action)) {
            String query = valueAfter(text, "QUERY:");
            int limit = parseInt(valueAfter(text, "LIMIT:"), 5);
            return new SearchAction(query, Math.max(1, Math.min(10, limit)));
        }
        if ("FETCH".equalsIgnoreCase(action)) {
            return new FetchAction(valueAfter(text, "URL:"));
        }
        if ("READ_MEMORY".equalsIgnoreCase(action)) {
            return new ReadMemoryAction(valueAfter(text, "KEY:"));
        }
        if ("WRITE_MEMORY".equalsIgnoreCase(action)) {
            String key = valueAfter(text, "KEY:");
            String content = blockAfter(text, "CONTENT:");
            return new WriteMemoryAction(key, content);
        }
        String content = blockAfter(text, "CONTENT:");
        if (!content.isBlank()) {
            return new FinalAction(content);
        }
        return new FinalAction(text);
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

    private static String blockAfter(String text, String label) {
        int start = text.indexOf(label);
        if (start < 0) {
            return "";
        }
        return text.substring(start + label.length()).trim();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }
}
