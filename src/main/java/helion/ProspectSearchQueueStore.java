package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ProspectSearchQueueStore {
    private static final String HEADER = "id,query_template,region,city,industry,pass,status,last_run,next_run,results_seen,notes\n";
    private final AgentRegistry agentRegistry;

    public ProspectSearchQueueStore(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    public String add(String agentId, String queryTemplate, String region, String city, String industry, String notes) throws IOException {
        AgentProfile profile = requireProfile(agentId);
        Path file = queueFile(profile);
        ensureFile(file);
        List<ProspectSearchQueueItem> items = list(agentId);
        int id = items.stream().mapToInt(ProspectSearchQueueItem::id).max().orElse(0) + 1;
        ProspectSearchQueueItem item = new ProspectSearchQueueItem(
                id,
                queryTemplate,
                region,
                city,
                industry,
                1,
                "active",
                null,
                LocalDate.now(),
                0,
                notes);
        Files.writeString(file, item.toCsvRow() + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        return "Added search queue item " + id + " for " + agentId;
    }

    public List<ProspectSearchQueueItem> list(String agentId) throws IOException {
        AgentProfile profile = requireProfile(agentId);
        Path file = queueFile(profile);
        ensureFile(file);
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<ProspectSearchQueueItem> items = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            List<String> columns = parseCsvLine(line);
            if (columns.size() < 11) {
                continue;
            }
            items.add(new ProspectSearchQueueItem(
                    parseInt(columns.get(0), i),
                    columns.get(1),
                    columns.get(2),
                    columns.get(3),
                    columns.get(4),
                    parseInt(columns.get(5), 1),
                    columns.get(6),
                    parseDate(columns.get(7)),
                    parseDate(columns.get(8)),
                    parseInt(columns.get(9), 0),
                    columns.get(10)));
        }
        items.sort(Comparator.comparingInt(ProspectSearchQueueItem::id));
        return items;
    }

    public ProspectSearchQueueItem nextDue(String agentId) throws IOException {
        LocalDate today = LocalDate.now();
        return list(agentId).stream()
                .filter(item -> item.isDue(today))
                .sorted(Comparator.comparing((ProspectSearchQueueItem item) -> item.nextRun() == null ? LocalDate.MIN : item.nextRun())
                        .thenComparingInt(ProspectSearchQueueItem::id))
                .findFirst()
                .orElse(null);
    }

    public void markRun(String agentId, ProspectSearchQueueItem updated) throws IOException {
        AgentProfile profile = requireProfile(agentId);
        Path file = queueFile(profile);
        ensureFile(file);
        List<ProspectSearchQueueItem> items = list(agentId);
        List<String> rewritten = new ArrayList<>();
        rewritten.add(HEADER.trim());
        for (ProspectSearchQueueItem item : items) {
            ProspectSearchQueueItem row = item.id() == updated.id() ? updated : item;
            rewritten.add(row.toCsvRow());
        }
        Files.write(file, rewritten, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    public String renderList(String agentId) throws IOException {
        List<ProspectSearchQueueItem> items = list(agentId);
        if (items.isEmpty()) {
            return "No queued searches.";
        }
        StringBuilder out = new StringBuilder();
        for (ProspectSearchQueueItem item : items) {
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

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
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
