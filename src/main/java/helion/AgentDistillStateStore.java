package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class AgentDistillStateStore {
    private static final String FILE_NAME = ".distill_state.json";

    public AgentDistillState read(AgentProfile profile) throws IOException {
        Path file = stateFile(profile);
        if (file == null || !Files.exists(file)) {
            return AgentDistillState.empty();
        }
        String json = Files.readString(file, StandardCharsets.UTF_8).trim();
        if (json.isEmpty()) {
            return AgentDistillState.empty();
        }
        return new AgentDistillState(
                readDateTime(json, "lastCheckedAt"),
                readDateTime(json, "lastDistilledAt"),
                readDateTime(json, "latestSourceModifiedAt"),
                readString(json, "providerName"),
                readString(json, "providerModel"));
    }

    public void write(AgentProfile profile, AgentDistillState state) throws IOException {
        Path file = stateFile(profile);
        Files.createDirectories(file.getParent());
        Files.writeString(
                file,
                toJson(state),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private Path stateFile(AgentProfile profile) {
        return profile.distilledDir().resolve(FILE_NAME);
    }

    private String toJson(AgentDistillState state) {
        return "{"
                + "\"lastCheckedAt\":\"" + TextUtils.escapeJson(format(state.lastCheckedAt())) + "\","
                + "\"lastDistilledAt\":\"" + TextUtils.escapeJson(format(state.lastDistilledAt())) + "\","
                + "\"latestSourceModifiedAt\":\"" + TextUtils.escapeJson(format(state.latestSourceModifiedAt())) + "\","
                + "\"providerName\":\"" + TextUtils.escapeJson(state.providerName() == null ? "" : state.providerName()) + "\","
                + "\"providerModel\":\"" + TextUtils.escapeJson(state.providerModel() == null ? "" : state.providerModel()) + "\""
                + "}";
    }

    private static String format(java.time.LocalDateTime value) {
        return value == null ? "" : value.toString();
    }

    private static String readString(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            return "";
        }
        int valueStart = start + marker.length();
        return TextUtils.readJsonStringValue(json, valueStart);
    }

    private static java.time.LocalDateTime readDateTime(String json, String key) {
        String value = readString(json, key);
        if (value.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalDateTime.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
