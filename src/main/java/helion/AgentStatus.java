package helion;

import java.time.LocalDateTime;

public record AgentStatus(
        String executionState,
        int runIntervalSeconds,
        LocalDateTime lastRun,
        String rawText) {

    private static final int DEFAULT_RUN_INTERVAL_SECONDS = 300;

    public static AgentStatus parse(String rawStatus, HelionConfig config) {
        String text = rawStatus == null ? "" : rawStatus.trim();
        String executionState = firstMatchingValue(text, "Execution state:", "State:");
        if (executionState.isBlank()) {
            executionState = inferFromConfig(config);
        } else {
            executionState = normalizeExecutionState(executionState, config);
        }
        int runIntervalSeconds = parsePositiveInt(firstMatchingValue(text, "Run interval seconds:"), DEFAULT_RUN_INTERVAL_SECONDS);
        LocalDateTime lastRun = parseDateTime(firstMatchingValue(text, "Last run:"));
        return new AgentStatus(executionState, runIntervalSeconds, lastRun, text);
    }

    public static String inferFromConfig(HelionConfig config) {
        String manager = config.managerProvider() == null ? "" : config.managerProvider().trim().toLowerCase();
        String worker = config.workerProvider() == null ? "" : config.workerProvider().trim().toLowerCase();
        if (isCloudProvider(manager) || isCloudProvider(worker)) {
            return "cloud_running";
        }
        if (isLocalProvider(manager) || isLocalProvider(worker)) {
            return "local_running";
        }
        return "local_running";
    }

    public boolean isRunnable() {
        return "cloud_running".equals(executionState) || "local_running".equals(executionState);
    }

    public String renderedStatus() {
        if (rawText == null || rawText.isBlank()) {
            return generatedStatus();
        }
        StringBuilder out = new StringBuilder(rawText);
        if (!rawText.toLowerCase().contains("execution state:")) {
            out.insert(0, "Execution state: " + executionState + "\n\n");
        }
        if (!rawText.toLowerCase().contains("run interval seconds:")) {
            out.append("\n\nRun interval seconds: ").append(runIntervalSeconds);
        }
        if (lastRun != null && !rawText.toLowerCase().contains("last run:")) {
            out.append("\nLast run: ").append(lastRun);
        }
        return out.toString().trim();
    }

    public String withLastRunText(LocalDateTime when) {
        String base = rawText == null ? "" : rawText;
        String updated = replaceOrAppendLine(base, "Execution state:", executionState);
        updated = replaceOrAppendLine(updated, "Run interval seconds:", Integer.toString(runIntervalSeconds));
        updated = replaceOrAppendLine(updated, "Last run:", when.toString());
        return updated.trim();
    }

    public String withSettingsText(String nextExecutionState, int nextRunIntervalSeconds) {
        String normalizedState = normalizeExecutionState(nextExecutionState, null);
        int interval = nextRunIntervalSeconds > 0 ? nextRunIntervalSeconds : DEFAULT_RUN_INTERVAL_SECONDS;
        String base = rawText == null ? "" : rawText;
        String updated = replaceOrAppendLine(base, "Execution state:", normalizedState);
        updated = replaceOrAppendLine(updated, "Run interval seconds:", Integer.toString(interval));
        return updated.trim();
    }

    private String generatedStatus() {
        StringBuilder out = new StringBuilder();
        out.append("Execution state: ").append(executionState).append('\n');
        out.append("Run interval seconds: ").append(runIntervalSeconds);
        if (lastRun != null) {
            out.append('\n').append("Last run: ").append(lastRun);
        }
        return out.toString();
    }

    private static String firstMatchingValue(String text, String... labels) {
        for (String label : labels) {
            String value = valueAfter(text, label);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String valueAfter(String text, String label) {
        int start = text.toLowerCase().indexOf(label.toLowerCase());
        if (start < 0) {
            return "";
        }
        int valueStart = start + label.length();
        int valueEnd = text.indexOf('\n', valueStart);
        if (valueEnd < 0) {
            valueEnd = text.length();
        }
        return text.substring(valueStart, valueEnd).trim();
    }

    private static int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private static String replaceOrAppendLine(String text, String label, String value) {
        if (text == null || text.isBlank()) {
            return label + " " + value;
        }
        String lower = text.toLowerCase();
        String marker = label.toLowerCase();
        int start = lower.indexOf(marker);
        if (start < 0) {
            return text.trim() + "\n" + label + " " + value;
        }
        int lineEnd = text.indexOf('\n', start);
        if (lineEnd < 0) {
            return text.substring(0, start) + label + " " + value;
        }
        return text.substring(0, start) + label + " " + value + text.substring(lineEnd);
    }

    private static String normalizeExecutionState(String value, HelionConfig config) {
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "cloud_running", "local_running", "paused", "disabled" -> normalized;
            case "active" -> config == null ? "local_running" : inferFromConfig(config);
            default -> config == null ? "local_running" : inferFromConfig(config);
        };
    }

    private static boolean isCloudProvider(String provider) {
        return "openai".equals(provider);
    }

    private static boolean isLocalProvider(String provider) {
        return "llama.cpp".equals(provider) || "llamacpp".equals(provider) || "llama".equals(provider) || "demo".equals(provider);
    }
}
