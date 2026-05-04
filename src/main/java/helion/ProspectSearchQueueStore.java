package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public final class ProspectSearchQueueStore {
    private static final String HEADER = "id,query_template,region,city,industry,priority,pass,status,last_run,next_run,attempts,prospects_saved,last_success,notes\n";
    private static final DateTimeFormatter ARCHIVE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
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
                3,
                1,
                "active",
                null,
                LocalDateTime.now(),
                0,
                0,
                null,
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
            boolean hasMetricsColumns = columns.size() >= 14;
            boolean hasPriorityColumn = columns.size() >= 12;
            items.add(new ProspectSearchQueueItem(
                    parseInt(columns.get(0), i),
                    columns.get(1),
                    columns.get(2),
                    columns.get(3),
                    columns.get(4),
                    hasPriorityColumn ? parsePriority(columns.get(5), 3) : parsePriorityFromNotes(columns.get(10), 3),
                    parseInt(columns.get(hasPriorityColumn ? 6 : 5), 1),
                    columns.get(hasPriorityColumn ? 7 : 6),
                    parseLastRun(columns.get(hasPriorityColumn ? 8 : 7)),
                    parseNextRun(columns.get(hasPriorityColumn ? 9 : 8), columns.get(hasPriorityColumn ? 7 : 6)),
                    hasMetricsColumns ? parseInt(columns.get(10), 0) : 0,
                    hasMetricsColumns ? parseInt(columns.get(11), 0) : parseInt(columns.get(hasPriorityColumn ? 10 : 9), 0),
                    hasMetricsColumns ? parseLastRun(columns.get(12)) : null,
                    columns.get(hasMetricsColumns ? 13 : (hasPriorityColumn ? 11 : 10))));
        }
        items.sort(Comparator.comparingInt(ProspectSearchQueueItem::id));
        return items;
    }

    public ProspectSearchQueueItem nextDue(String agentId) throws IOException {
        List<ProspectSearchQueueItem> due = dueItems(agentId, 1);
        return due.isEmpty() ? null : due.get(0);
    }

    public List<ProspectSearchQueueItem> dueItems(String agentId, int limit) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        return list(agentId).stream()
                .filter(item -> item.isDue(now))
                .sorted(Comparator.comparingInt((ProspectSearchQueueItem item) -> item.priority() <= 0 ? 3 : item.priority())
                        .thenComparing((ProspectSearchQueueItem item) -> item.nextRun() == null ? LocalDateTime.MIN : item.nextRun())
                        .thenComparingInt(ProspectSearchQueueItem::id))
                .limit(Math.max(1, limit))
                .toList();
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

    public String seedFromCities(String agentId) throws IOException {
        AgentProfile profile = requireProfile(agentId);
        Path citiesFile = profile.workspaceDir().resolve("cities.csv");
        Path categoriesFile = profile.distilledDir().resolve("market_categories.md");
        if (!Files.exists(citiesFile)) {
            return "No cities.csv found at " + citiesFile;
        }
        List<String> categoryTemplates = loadCategoryTemplates(categoriesFile);

        List<ProspectSearchQueueItem> existing = list(agentId);
        Set<String> existingKeys = new HashSet<>();
        for (ProspectSearchQueueItem item : existing) {
            existingKeys.add(seedKey(item.queryTemplate(), item.region(), item.city()));
        }

        List<String> lines = Files.readAllLines(citiesFile, StandardCharsets.UTF_8);
        if (lines.size() <= 1) {
            return "No city seed rows found in " + citiesFile;
        }

        Path file = queueFile(profile);
        ensureFile(file);
        int nextId = existing.stream().mapToInt(ProspectSearchQueueItem::id).max().orElse(0) + 1;
        List<String> appended = new ArrayList<>();
        int created = 0;
        int skipped = 0;

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            List<String> columns = parseCsvLine(line);
            if (columns.size() < 6) {
                continue;
            }
            String country = columns.get(0).trim();
            String region = columns.get(1).trim();
            String city = columns.get(2).trim();
            String priority = columns.get(3).trim();
            String focus = columns.get(4).trim();
            String notes = columns.get(5).trim();
            if (city.isBlank() || region.isBlank() || focus.isBlank()) {
                continue;
            }
            Set<String> queryTemplates = new HashSet<>();
            for (String rawFocus : focus.split("\\|")) {
                String queryTemplate = rawFocus == null ? "" : rawFocus.trim();
                if (queryTemplate.isBlank()) {
                    continue;
                }
                queryTemplates.add(queryTemplate);
            }
            if (!categoryTemplates.isEmpty()) {
                int categoryLimit = categoryTemplateLimit(priority);
                for (int c = 0; c < Math.min(categoryLimit, categoryTemplates.size()); c++) {
                    queryTemplates.add(categoryTemplates.get(c));
                }
            }
            for (String queryTemplate : queryTemplates) {
                String key = seedKey(queryTemplate, region, city);
                if (existingKeys.contains(key)) {
                    skipped++;
                    continue;
                }
                String combinedNotes = "Priority: " + (priority.isBlank() ? "unspecified" : priority)
                        + " | Country: " + (country.isBlank() ? "unspecified" : country)
                        + (notes.isBlank() ? "" : " | " + notes);
                ProspectSearchQueueItem item = new ProspectSearchQueueItem(
                        nextId++,
                        queryTemplate,
                        region,
                        city,
                        "",
                        parsePriority(priority, 3),
                        1,
                        "active",
                        null,
                        LocalDateTime.now(),
                        0,
                        0,
                        null,
                        combinedNotes);
                appended.add(item.toCsvRow());
                existingKeys.add(key);
                created++;
            }
        }

        if (!appended.isEmpty()) {
            Files.writeString(file, String.join("\n", appended) + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        }
        return "Seeded " + created + " queue items from " + citiesFile + ". Skipped " + skipped + " duplicates.";
    }

    public String rebuildFromCities(String agentId) throws IOException {
        AgentProfile profile = requireProfile(agentId);
        Path file = queueFile(profile);
        ensureFile(file);
        Path archive = archiveQueueFile(file);
        Files.writeString(file, HEADER, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        String seedResult = seedFromCities(agentId);
        return "Archived previous queue to " + archive + ". " + seedResult;
    }

    private static int categoryTemplateLimit(String priority) {
        int value = parseInt(priority, 3);
        return switch (value) {
            case 1 -> 5;
            case 2 -> 3;
            default -> 2;
        };
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

    private static Path archiveQueueFile(Path file) throws IOException {
        Path parent = file.getParent();
        Files.createDirectories(parent);
        String archiveName = "search_queue.archive." + LocalDateTime.now().format(ARCHIVE_STAMP) + ".csv";
        Path archive = parent.resolve(archiveName);
        Files.copy(file, archive, StandardCopyOption.REPLACE_EXISTING);
        return archive;
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

    private static int parsePriority(String value, int fallback) {
        int parsed = parseInt(value, fallback);
        if (parsed < 1 || parsed > 9) {
            return fallback;
        }
        return parsed;
    }

    private static int parsePriorityFromNotes(String notes, int fallback) {
        if (notes == null || notes.isBlank()) {
            return fallback;
        }
        String marker = "priority:";
        String lower = notes.toLowerCase();
        int start = lower.indexOf(marker);
        if (start < 0) {
            return fallback;
        }
        start += marker.length();
        while (start < lower.length() && Character.isWhitespace(lower.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < lower.length() && Character.isDigit(lower.charAt(end))) {
            end++;
        }
        if (end == start) {
            return fallback;
        }
        return parsePriority(lower.substring(start, end), fallback);
    }

    private static LocalDateTime parseLastRun(String value) {
        return parseDateTime(value, false);
    }

    private static LocalDateTime parseNextRun(String value, String status) {
        return parseDateTime(value, isLegacyActiveStatus(status));
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
            if (forceDueForLegacyDate) {
                return LocalDateTime.MIN;
            }
            return date.atStartOfDay();
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean isLegacyActiveStatus(String status) {
        return status != null && status.equalsIgnoreCase("active");
    }

    private static List<String> loadCategoryTemplates(Path categoriesFile) throws IOException {
        if (categoriesFile == null || !Files.exists(categoriesFile)) {
            return List.of();
        }
        List<String> lines = Files.readAllLines(categoriesFile, StandardCharsets.UTF_8);
        List<String> templates = new ArrayList<>();
        String section = "";
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.startsWith("## ")) {
                section = line.substring(3).trim().toLowerCase();
                continue;
            }
            if (!line.startsWith("- ")) {
                continue;
            }
            if (!section.equals("strongest near-term buyer categories")
                    && !section.equals("broader fabrication and manufacturing categories")) {
                continue;
            }
            String template = normalizeCategoryTemplate(line.substring(2).trim());
            if (!template.isBlank() && !templates.contains(template)) {
                templates.add(template);
            }
        }
        return templates;
    }

    private static String normalizeCategoryTemplate(String value) {
        String text = value == null ? "" : value.trim().toLowerCase();
        if (text.isBlank()) {
            return "";
        }
        text = text.replace("/", " ");
        text = text.replace(" and ", " ");
        text = text.replace(" with ", " ");
        text = text.replace(" doing ", " ");
        text = text.replace(" producing ", " ");
        text = text.replace(" making ", " ");
        text = text.replace(" in-house ", " ");
        text = text.replace(" repeated ", " ");
        text = text.replace(" repeat ", " ");
        text = text.replace(" work", "");
        text = text.replace(" shops", "");
        text = text.replace(" shop", "");
        text = text.replace(" builders", " builder");
        text = text.replace(" fabricators", " fabrication");
        text = text.replace(" manufacturers", " manufacturer");
        text = text.replace(" assemblies", " assemblies");
        text = text.replace(" tubular", " tubular");
        return text.replaceAll("\\s+", " ").trim();
    }

    private static String seedKey(String queryTemplate, String region, String city) {
        return (queryTemplate == null ? "" : queryTemplate.trim().toLowerCase()) + "|"
                + (region == null ? "" : region.trim().toLowerCase()) + "|"
                + (city == null ? "" : city.trim().toLowerCase());
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
