package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AgentActivityStore {
    private static final Duration RETENTION = Duration.ofHours(48);

    public void append(AgentProfile profile, AgentActivityEntry entry) throws IOException {
        Path file = profile.activityFile();
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        pruneExpired(file);
        Files.writeString(
                file,
                entry.toJson() + "\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE);
    }

    public List<AgentActivityEntry> list(AgentProfile profile, int limit) throws IOException {
        if (profile == null) {
            return List.of();
        }
        Path file = profile.activityFile();
        if (file == null || !Files.exists(file)) {
            return List.of();
        }
        List<AgentActivityEntry> entries = readEntries(file);
        entries.sort(Comparator.comparing(AgentActivityEntry::timestamp, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        if (limit > 0 && entries.size() > limit) {
            return new ArrayList<>(entries.subList(0, limit));
        }
        return entries;
    }

    private void pruneExpired(Path file) throws IOException {
        if (file == null || !Files.exists(file)) {
            return;
        }
        List<AgentActivityEntry> entries = readEntries(file);
        LocalDateTime cutoff = LocalDateTime.now().minus(RETENTION);
        List<String> kept = new ArrayList<>();
        for (AgentActivityEntry entry : entries) {
            if (entry.timestamp() == null || !entry.timestamp().isBefore(cutoff)) {
                kept.add(entry.toJson());
            }
        }
        if (kept.size() == entries.size()) {
            return;
        }
        Files.write(
                file,
                kept,
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private List<AgentActivityEntry> readEntries(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<AgentActivityEntry> entries = new ArrayList<>();
        for (String line : lines) {
            String json = line == null ? "" : line.trim();
            if (json.isEmpty()) {
                continue;
            }
            entries.add(new AgentActivityEntry(
                    readDateTime(json, "timestamp"),
                    readString(json, "level"),
                    readString(json, "task"),
                    readString(json, "summary"),
                    readString(json, "details")));
        }
        return entries;
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

    private static LocalDateTime readDateTime(String json, String key) {
        String value = readString(json, key);
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
