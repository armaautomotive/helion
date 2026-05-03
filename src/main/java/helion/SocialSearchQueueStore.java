package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SocialSearchQueueStore {
    private static final String HEADER = "id,site,topic,audience,query_template,pass,status,last_run,next_run,results_seen,notes\n";
    private final AgentRegistry agentRegistry;

    public SocialSearchQueueStore(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    public String add(String agentId, String site, String topic, String audience, String queryTemplate, String notes) throws IOException {
        AgentProfile profile = requireProfile(agentId);
        Path file = queueFile(profile);
        ensureFile(file);
        List<SocialSearchQueueItem> items = list(agentId);
        int id = items.stream().mapToInt(SocialSearchQueueItem::id).max().orElse(0) + 1;
        SocialSearchQueueItem item = new SocialSearchQueueItem(
                id, site, topic, audience, queryTemplate, 1, "active", null, LocalDateTime.now(), 0, notes);
        Files.writeString(file, item.toCsvRow() + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        return "Added social search queue item " + id + " for " + agentId;
    }

    public List<SocialSearchQueueItem> list(String agentId) throws IOException {
        AgentProfile profile = requireProfile(agentId);
        Path file = queueFile(profile);
        ensureFile(file);
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<SocialSearchQueueItem> items = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            List<String> columns = parseCsvLine(line);
            if (columns.size() < 11) {
                continue;
            }
            items.add(new SocialSearchQueueItem(
                    parseInt(columns.get(0), i),
                    columns.get(1),
                    columns.get(2),
                    columns.get(3),
                    columns.get(4),
                    parseInt(columns.get(5), 1),
                    columns.get(6),
                    parseLastRun(columns.get(7)),
                    parseNextRun(columns.get(8), columns.get(6)),
                    parseInt(columns.get(9), 0),
                    columns.get(10)));
        }
        items.sort(Comparator.comparingInt(SocialSearchQueueItem::id));
        return items;
    }

    public SocialSearchQueueItem nextDue(String agentId) throws IOException {
        List<SocialSearchQueueItem> due = dueItems(agentId, 1);
        return due.isEmpty() ? null : due.get(0);
    }

    public List<SocialSearchQueueItem> dueItems(String agentId, int limit) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        return list(agentId).stream()
                .filter(item -> item.isDue(now))
                .sorted(Comparator.comparing((SocialSearchQueueItem item) -> item.nextRun() == null ? LocalDateTime.MIN : item.nextRun())
                        .thenComparingInt(SocialSearchQueueItem::id))
                .limit(Math.max(1, limit))
                .toList();
    }

    public void markRun(String agentId, SocialSearchQueueItem updated) throws IOException {
        AgentProfile profile = requireProfile(agentId);
        Path file = queueFile(profile);
        ensureFile(file);
        List<SocialSearchQueueItem> items = list(agentId);
        List<String> rewritten = new ArrayList<>();
        rewritten.add(HEADER.trim());
        for (SocialSearchQueueItem item : items) {
            rewritten.add((item.id() == updated.id() ? updated : item).toCsvRow());
        }
        Files.write(file, rewritten, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    public String renderList(String agentId) throws IOException {
        List<SocialSearchQueueItem> items = list(agentId);
        if (items.isEmpty()) {
            return "No queued social searches.";
        }
        StringBuilder out = new StringBuilder();
        for (SocialSearchQueueItem item : items) {
            out.append(item.summaryLine()).append('\n');
        }
        return out.toString().trim();
    }

    private AgentProfile requireProfile(String agentId) {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown agent: " + agentId);
        }
        return profile;
    }

    private Path queueFile(AgentProfile profile) {
        return profile.workspaceDir().resolve("search_queue.csv");
    }

    private static void ensureFile(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            Files.writeString(file, HEADER, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static LocalDateTime parseLastRun(String value) {
        return parseDateTime(value, false);
    }

    private static LocalDateTime parseNextRun(String value, String status) {
        return parseDateTime(value, status != null && status.equalsIgnoreCase("active"));
    }

    private static LocalDateTime parseDateTime(String value, boolean forceDueForLegacyDate) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String text = value.trim();
            if (text.contains("T")) {
                return LocalDateTime.parse(text);
            }
            LocalDate date = LocalDate.parse(text);
            return forceDueForLegacyDate ? LocalDateTime.MIN : date.atStartOfDay();
        } catch (Exception ex) {
            return null;
        }
    }

    private static List<String> parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (c == ',' && !inQuotes) {
                columns.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        columns.add(current.toString());
        return columns;
    }
}
