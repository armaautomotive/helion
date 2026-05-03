package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
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
                LocalDate.now().toString(),
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
        Map<String, MutableSummary> grouped = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        if (!Files.exists(eventsFile)) {
            return List.of();
        }
        for (String line : Files.readAllLines(eventsFile, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split("\t");
            if (parts.length < 7) {
                continue;
            }
            LocalDate date = LocalDate.parse(parts[0]);
            String provider = parts[1];
            String model = parts[2];
            long prompt = Long.parseLong(parts[3]);
            long completion = Long.parseLong(parts[4]);
            long total = Long.parseLong(parts[5]);
            boolean exact = "1".equals(parts[6]);
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
}
