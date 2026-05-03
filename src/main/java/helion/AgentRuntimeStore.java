package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public final class AgentRuntimeStore {
    public AgentRuntime read(AgentProfile profile, String executionState) throws IOException {
        if (profile == null) {
            throw new IllegalArgumentException("Profile is required.");
        }
        Path file = profile.runtimeFile();
        if (file == null || !Files.exists(file)) {
            return AgentRuntime.initial(profile.id(), executionState);
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        return new AgentRuntime(
                profile.id(),
                readString(json, "executionState", executionState),
                readString(json, "runtimeState", "idle"),
                readString(json, "currentTask", ""),
                readDateTime(json, "lastTaskStartedAt"),
                readDateTime(json, "lastTaskFinishedAt"),
                readDateTime(json, "lastSuccessAt"),
                readDateTime(json, "lastErrorAt"),
                readInt(json, "consecutiveFailures", 0),
                readInt(json, "totalSuccesses", 0),
                readInt(json, "totalFailures", 0),
                readString(json, "lastResultSummary", ""),
                readString(json, "lastOutputPath", ""),
                readLong(json, "averageCycleSeconds", 0),
                readString(json, "lastErrorMessage", ""));
    }

    public void write(AgentProfile profile, AgentRuntime runtime) throws IOException {
        Path file = profile.runtimeFile();
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(
                file,
                runtime.toJson(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private static String readString(String json, String key, String fallback) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            return fallback;
        }
        int valueStart = start + marker.length();
        return TextUtils.readJsonStringValue(json, valueStart);
    }

    private static int readInt(String json, String key, int fallback) {
        String raw = readNumber(json, key);
        try {
            return Integer.parseInt(raw);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static long readLong(String json, String key, long fallback) {
        String raw = readNumber(json, key);
        try {
            return Long.parseLong(raw);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static String readNumber(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) {
            return "";
        }
        int valueStart = start + marker.length();
        int valueEnd = valueStart;
        while (valueEnd < json.length() && (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '-')) {
            valueEnd++;
        }
        return json.substring(valueStart, valueEnd);
    }

    private static LocalDateTime readDateTime(String json, String key) {
        String value = readString(json, key, "");
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
