package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SocialOpportunityStore {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String CSV_HEADER = "title,url,site,community,author,posted,relevance,buyer_signal,product_fit,recommended_angle,evidence,status,tags,source_urls\n";
    private final AgentRegistry agentRegistry;
    private final HelionConfig config;

    public SocialOpportunityStore(AgentRegistry agentRegistry, HelionConfig config) {
        this.agentRegistry = agentRegistry;
        this.config = config;
    }

    public String save(String agentId, String searchFocus, List<SocialOpportunityRecord> records) throws IOException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown agent: " + agentId);
        }
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("No social opportunities to save.");
        }
        Files.createDirectories(profile.workspaceDir());
        Path markdownFile = AgentOutputResolver.resolvePrimaryOutputFile(profile, config, "workspace/opportunities.md");
        Path csvFile = profile.workspaceDir().resolve("opportunities.csv");
        if (markdownFile.getParent() != null) {
            Files.createDirectories(markdownFile.getParent());
        }
        if (!Files.exists(markdownFile)) {
            Files.writeString(markdownFile, "# Social Opportunities\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        }
        ensureCsvHeader(csvFile);

        Set<String> existingKeys = readExistingKeys(csvFile);
        List<SocialOpportunityRecord> unique = filterNewRecords(records, existingKeys);
        int skipped = records.size() - unique.size();
        if (unique.isEmpty()) {
            return "Saved 0 social opportunities. Skipped " + skipped + " duplicates already present in " + markdownFile;
        }

        StringBuilder markdown = new StringBuilder();
        markdown.append('\n');
        markdown.append("### Social Scan ").append(LocalDateTime.now().format(STAMP)).append('\n');
        markdown.append("- Search focus: ").append(searchFocus == null || searchFocus.isBlank() ? "(unspecified)" : searchFocus.trim()).append('\n');
        markdown.append("- Opportunities added: ").append(unique.size()).append('\n');
        markdown.append("- Duplicates skipped: ").append(skipped).append('\n').append('\n');
        for (SocialOpportunityRecord record : unique) {
            markdown.append(record.toMarkdown()).append('\n').append('\n');
        }
        Files.writeString(markdownFile, markdown.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        StringBuilder csv = new StringBuilder();
        for (SocialOpportunityRecord record : unique) {
            csv.append(record.toCsvRow()).append('\n');
        }
        Files.writeString(csvFile, csv.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        return "Saved " + unique.size() + " social opportunities. Skipped " + skipped + " duplicates. Updated " + markdownFile + " and " + csvFile;
    }

    private static void ensureCsvHeader(Path csvFile) throws IOException {
        if (!Files.exists(csvFile)) {
            Files.writeString(csvFile, CSV_HEADER, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        }
    }

    private static Set<String> readExistingKeys(Path csvFile) throws IOException {
        Set<String> keys = new HashSet<>();
        if (!Files.exists(csvFile)) {
            return keys;
        }
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            List<String> columns = parseCsvLine(line);
            if (columns.size() < 14) {
                continue;
            }
            SocialOpportunityRecord record = new SocialOpportunityRecord(
                    columns.get(0), columns.get(1), columns.get(2), columns.get(3), columns.get(4), columns.get(5),
                    columns.get(6), columns.get(7), columns.get(8), columns.get(9), columns.get(10), columns.get(11),
                    splitPipe(columns.get(12)), splitPipe(columns.get(13)));
            keys.addAll(record.dedupeKeys());
        }
        return keys;
    }

    private static List<SocialOpportunityRecord> filterNewRecords(List<SocialOpportunityRecord> records, Set<String> existingKeys) {
        List<SocialOpportunityRecord> unique = new ArrayList<>();
        for (SocialOpportunityRecord record : records) {
            if (record == null) {
                continue;
            }
            boolean duplicate = false;
            for (String key : record.dedupeKeys()) {
                if (existingKeys.contains(key)) {
                    duplicate = true;
                    break;
                }
            }
            if (duplicate) {
                continue;
            }
            existingKeys.addAll(record.dedupeKeys());
            unique.add(record);
        }
        return unique;
    }

    private static List<String> splitPipe(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] parts = value.split("\\|");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                out.add(part.trim());
            }
        }
        return out;
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
