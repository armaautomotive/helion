package helion;

import java.time.LocalDateTime;

public record AgentStatus(
        String runState,
        String executionTarget,
        String preferredLocalPool,
        int runIntervalSeconds,
        LocalDateTime lastRun,
        String primaryOutputFile,
        String rawText) {

    private static final int DEFAULT_RUN_INTERVAL_SECONDS = 300;

    public static AgentStatus parse(String rawStatus, HelionConfig config) {
        String text = rawStatus == null ? "" : rawStatus.trim();
        String runState = normalizeRunState(firstMatchingValue(text, "Run state:"));
        String executionTarget = normalizeExecutionTarget(firstMatchingValue(text, "Execution target:"), config);
        String preferredLocalPool = normalizePreferredLocalPool(firstMatchingValue(text, "Preferred local pool:"), config);
        int runIntervalSeconds = parsePositiveInt(firstMatchingValue(text, "Run interval seconds:"), DEFAULT_RUN_INTERVAL_SECONDS);
        LocalDateTime lastRun = parseDateTime(firstMatchingValue(text, "Last run:"));
        String primaryOutputFile = normalizeOutputFile(firstMatchingValue(text, "Primary output file:"));
        return new AgentStatus(runState, executionTarget, preferredLocalPool, runIntervalSeconds, lastRun, primaryOutputFile, text);
    }

    public static String inferExecutionTargetFromConfig(HelionConfig config) {
        String manager = config.managerProvider() == null ? "" : config.managerProvider().trim().toLowerCase();
        String worker = config.workerProvider() == null ? "" : config.workerProvider().trim().toLowerCase();
        if (isCloudProvider(manager) || isCloudProvider(worker)) {
            return "cloud";
        }
        return "local";
    }

    public boolean isRunnable() {
        return "running".equals(runState);
    }

    public String renderedStatus() {
        if (rawText == null || rawText.isBlank()) {
            return generatedStatus();
        }
        StringBuilder out = new StringBuilder(rawText);
        ensureLine(out, "Run state:", runState);
        ensureLine(out, "Execution target:", executionTarget);
        ensureLine(out, "Preferred local pool:", preferredLocalPool);
        ensureLine(out, "Run interval seconds:", Integer.toString(runIntervalSeconds));
        if (!primaryOutputFile.isBlank()) {
            ensureLine(out, "Primary output file:", primaryOutputFile);
        }
        if (lastRun != null) {
            ensureLine(out, "Last run:", lastRun.toString());
        }
        return out.toString().trim();
    }

    public String withLastRunText(LocalDateTime when) {
        String base = rawText == null ? "" : rawText;
        String updated = replaceOrAppendLine(base, "Run state:", runState);
        updated = replaceOrAppendLine(updated, "Execution target:", executionTarget);
        updated = replaceOrAppendLine(updated, "Preferred local pool:", preferredLocalPool);
        updated = replaceOrAppendLine(updated, "Run interval seconds:", Integer.toString(runIntervalSeconds));
        updated = replaceOrAppendLine(updated, "Primary output file:", primaryOutputFile);
        updated = replaceOrAppendLine(updated, "Last run:", when.toString());
        return updated.trim();
    }

    public String withSettingsText(
            String nextRunState,
            String nextExecutionTarget,
            String nextPreferredLocalPool,
            int nextRunIntervalSeconds,
            String nextPrimaryOutputFile) {
        String normalizedState = normalizeRunState(nextRunState);
        String normalizedTarget = normalizeExecutionTarget(nextExecutionTarget, null);
        String normalizedPool = normalizePreferredLocalPool(nextPreferredLocalPool, null);
        int interval = nextRunIntervalSeconds > 0 ? nextRunIntervalSeconds : DEFAULT_RUN_INTERVAL_SECONDS;
        String outputFile = normalizeOutputFile(nextPrimaryOutputFile);
        String base = rawText == null ? "" : rawText;
        String updated = replaceOrAppendLine(base, "Run state:", normalizedState);
        updated = replaceOrAppendLine(updated, "Execution target:", normalizedTarget);
        updated = replaceOrAppendLine(updated, "Preferred local pool:", normalizedPool);
        updated = replaceOrAppendLine(updated, "Run interval seconds:", Integer.toString(interval));
        updated = replaceOrAppendLine(updated, "Primary output file:", outputFile);
        return updated.trim();
    }

    private String generatedStatus() {
        StringBuilder out = new StringBuilder();
        out.append("Run state: ").append(runState).append('\n');
        out.append("Execution target: ").append(executionTarget).append('\n');
        out.append("Preferred local pool: ").append(preferredLocalPool).append('\n');
        out.append("Run interval seconds: ").append(runIntervalSeconds).append('\n');
        if (!primaryOutputFile.isBlank()) {
            out.append("Primary output file: ").append(primaryOutputFile).append('\n');
        }
        if (lastRun != null) {
            out.append("Last run: ").append(lastRun).append('\n');
        }
        return out.toString().trim();
    }

    private static void ensureLine(StringBuilder text, String label, String value) {
        String lower = text.toString().toLowerCase();
        if (!lower.contains(label.toLowerCase())) {
            if (text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
                text.append('\n');
            }
            text.append(label).append(' ').append(value);
        }
    }

    private static String firstMatchingValue(String text, String label) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String lower = text.toLowerCase();
        String marker = label.toLowerCase();
        int start = lower.indexOf(marker);
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

    private static String normalizeRunState(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return switch (normalized) {
            case "paused", "disabled" -> normalized;
            default -> "running";
        };
    }

    private static String normalizeExecutionTarget(String value, HelionConfig config) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return switch (normalized) {
            case "cloud", "local" -> normalized;
            default -> config == null ? "local" : inferExecutionTargetFromConfig(config);
        };
    }

    private static String normalizePreferredLocalPool(String value, HelionConfig config) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if (!normalized.isBlank()) {
            return normalized;
        }
        if (config != null && config.defaultLocalPool() != null && !config.defaultLocalPool().isBlank()) {
            return config.defaultLocalPool().trim().toLowerCase();
        }
        return "default";
    }

    private static String normalizeOutputFile(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("workspace/")) {
            return normalized;
        }
        if (normalized.isBlank()) {
            return "";
        }
        return "workspace/" + normalized;
    }

    private static boolean isCloudProvider(String provider) {
        return "openai".equals(provider);
    }
}
