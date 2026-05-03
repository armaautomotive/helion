package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UsageTracker {
    private final Path eventsFile;

    public UsageTracker(Path eventsFile) {
        this.eventsFile = eventsFile;
    }

    public synchronized void record(UsageMetrics usage) throws IOException {
        if (usage == null) {
            return;
        }
        if (eventsFile.getParent() != null) {
            Files.createDirectories(eventsFile.getParent());
        }
        String line = String.join("\t",
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                safe(UsageContext.currentAgentId()),
                safe(usage.provider()),
                safe(usage.model()),
                Integer.toString(usage.promptTokens()),
                Integer.toString(usage.completionTokens()),
                Integer.toString(usage.totalTokens()),
                usage.exact() ? "1" : "0");
        Files.writeString(eventsFile, line + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public synchronized List<UsageSummary> summarize() throws IOException {
        return summarize("");
    }

    public synchronized List<UsageSummary> summarize(String agentId) throws IOException {
        Map<String, MutableSummary> grouped = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        if (!Files.exists(eventsFile)) {
            return List.of();
        }
        for (String line : Files.readAllLines(eventsFile, StandardCharsets.UTF_8)) {
            UsageEvent event = parseEvent(line);
            if (event == null) {
                continue;
            }
            if (!matchesAgent(event, agentId)) {
                continue;
            }
            LocalDate date = event.timestamp().toLocalDate();
            String provider = event.provider();
            String model = event.model();
            long prompt = event.promptTokens();
            long completion = event.completionTokens();
            long total = event.totalTokens();
            boolean exact = event.exact();
            String key = provider + "\u0000" + model;
            MutableSummary summary = grouped.computeIfAbsent(key, ignored -> new MutableSummary(provider, model));
            summary.requests++;
            summary.allExact = summary.allExact && exact;
            if (date.equals(today)) {
                summary.dailyPrompt += prompt;
                summary.dailyCompletion += completion;
                summary.dailyTotal += total;
            }
            if (date.getYear() == today.getYear() && date.getMonth() == today.getMonth()) {
                summary.monthlyPrompt += prompt;
                summary.monthlyCompletion += completion;
                summary.monthlyTotal += total;
            }
            if (date.getYear() == today.getYear()) {
                summary.yearlyPrompt += prompt;
                summary.yearlyCompletion += completion;
                summary.yearlyTotal += total;
            }
        }

        List<UsageSummary> summaries = new ArrayList<>();
        for (MutableSummary summary : grouped.values()) {
            summaries.add(summary.freeze());
        }
        return summaries;
    }

    public synchronized List<UsagePoint> hourlySeriesLast24Hours() throws IOException {
        return hourlySeriesLast24Hours("");
    }

    public synchronized List<UsagePoint> hourlySeriesLast24Hours(String agentId) throws IOException {
        LocalDateTime currentHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        List<UsagePoint> points = new ArrayList<>();
        Map<LocalDateTime, MutablePoint> buckets = new LinkedHashMap<>();
        for (int i = 23; i >= 0; i--) {
            LocalDateTime bucketTime = currentHour.minusHours(i);
            buckets.put(bucketTime, new MutablePoint(bucketTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))));
        }
        for (UsageEvent event : readEvents()) {
            if (!matchesAgent(event, agentId)) {
                continue;
            }
            LocalDateTime bucketKey = event.timestamp().truncatedTo(ChronoUnit.HOURS);
            MutablePoint point = buckets.get(bucketKey);
            if (point == null) {
                continue;
            }
            point.totalTokens += event.totalTokens();
            point.requests++;
        }
        for (MutablePoint point : buckets.values()) {
            points.add(point.freeze());
        }
        return points;
    }

    public synchronized List<UsagePoint> dailySeriesLast30Days() throws IOException {
        return dailySeriesLast30Days("");
    }

    public synchronized List<UsagePoint> dailySeriesLast30Days(String agentId) throws IOException {
        LocalDate today = LocalDate.now();
        List<UsagePoint> points = new ArrayList<>();
        Map<LocalDate, MutablePoint> buckets = new LinkedHashMap<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate bucketDate = today.minusDays(i);
            buckets.put(bucketDate, new MutablePoint(bucketDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))));
        }
        for (UsageEvent event : readEvents()) {
            if (!matchesAgent(event, agentId)) {
                continue;
            }
            LocalDate bucketKey = event.timestamp().toLocalDate();
            MutablePoint point = buckets.get(bucketKey);
            if (point == null) {
                continue;
            }
            point.totalTokens += event.totalTokens();
            point.requests++;
        }
        for (MutablePoint point : buckets.values()) {
            points.add(point.freeze());
        }
        return points;
    }

    public String renderText() throws IOException {
        List<UsageSummary> summaries = summarize();
        if (summaries.isEmpty()) {
            return "No usage recorded yet.";
        }
        StringBuilder out = new StringBuilder();
        for (UsageSummary summary : summaries) {
            if (out.length() > 0) {
                out.append("\n\n");
            }
            out.append(summary.provider()).append(" / ").append(summary.model()).append('\n');
            out.append("  daily: ").append(summary.dailyTotalTokens()).append(" tokens\n");
            out.append("  monthly: ").append(summary.monthlyTotalTokens()).append(" tokens\n");
            out.append("  yearly: ").append(summary.yearlyTotalTokens()).append(" tokens\n");
            out.append("  requests: ").append(summary.requests()).append('\n');
            out.append("  exact: ").append(summary.allExact() ? "yes" : "mixed/estimated");
        }
        return out.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('\t', ' ').trim();
    }

    private List<UsageEvent> readEvents() throws IOException {
        if (!Files.exists(eventsFile)) {
            return List.of();
        }
        List<UsageEvent> events = new ArrayList<>();
        for (String line : Files.readAllLines(eventsFile, StandardCharsets.UTF_8)) {
            UsageEvent event = parseEvent(line);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    private static UsageEvent parseEvent(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String[] parts = trimmed.split("\t");
        if (parts.length < 7) {
            return null;
        }
        try {
            LocalDateTime timestamp = parseTimestamp(parts[0]);
            return new UsageEvent(
                    timestamp,
                    parts.length >= 8 ? parts[2] : parts[1],
                    parts.length >= 8 ? parts[3] : parts[2],
                    Long.parseLong(parts.length >= 8 ? parts[4] : parts[3]),
                    Long.parseLong(parts.length >= 8 ? parts[5] : parts[4]),
                    Long.parseLong(parts.length >= 8 ? parts[6] : parts[5]),
                    "1".equals(parts.length >= 8 ? parts[7] : parts[6]),
                    parts.length >= 8 ? parts[1] : "");
        } catch (Exception ex) {
            return null;
        }
    }

    private static LocalDateTime parseTimestamp(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.contains("T")) {
            return LocalDateTime.parse(value);
        }
        return LocalDate.parse(value).atStartOfDay();
    }

    private static boolean matchesAgent(UsageEvent event, String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return true;
        }
        return agentId.trim().equalsIgnoreCase(event.agentId());
    }

    private static final class MutableSummary {
        private final String provider;
        private final String model;
        private long dailyPrompt;
        private long dailyCompletion;
        private long dailyTotal;
        private long monthlyPrompt;
        private long monthlyCompletion;
        private long monthlyTotal;
        private long yearlyPrompt;
        private long yearlyCompletion;
        private long yearlyTotal;
        private long requests;
        private boolean allExact = true;

        private MutableSummary(String provider, String model) {
            this.provider = provider;
            this.model = model;
        }

        private UsageSummary freeze() {
            return new UsageSummary(
                    provider,
                    model,
                    dailyPrompt,
                    dailyCompletion,
                    dailyTotal,
                    monthlyPrompt,
                    monthlyCompletion,
                    monthlyTotal,
                    yearlyPrompt,
                    yearlyCompletion,
                    yearlyTotal,
                    requests,
                    allExact);
        }
    }

    private static final class MutablePoint {
        private final String label;
        private long totalTokens;
        private long requests;

        private MutablePoint(String label) {
            this.label = label;
        }

        private UsagePoint freeze() {
            return new UsagePoint(label, totalTokens, requests);
        }
    }
}
