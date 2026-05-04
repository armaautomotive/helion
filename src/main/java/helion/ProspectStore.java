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

public final class ProspectStore {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String CSV_HEADER = "company,website,contact_name,contact_role,contact_email,phone,location,industry,fit_score,status,priority,owner,discovered_at,last_updated,tags,why_fit,evidence,source_urls,next_action\n";
    private static final String LEGACY_CSV_HEADER = "company,website,contact_name,contact_role,contact_email,location,industry,fit_score,why_fit,evidence,source_urls,next_action";
    private final AgentRegistry agentRegistry;
    private final HelionConfig config;

    public ProspectStore(AgentRegistry agentRegistry, HelionConfig config) {
        this.agentRegistry = agentRegistry;
        this.config = config;
    }

    public String save(String agentId, String searchFocus, List<ProspectRecord> records) throws IOException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown agent: " + agentId);
        }
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("No prospects to save.");
        }
        Files.createDirectories(profile.workspaceDir());
        Path markdownFile = AgentOutputResolver.resolvePrimaryOutputFile(profile, config, "workspace/prospects.md");
        Path csvFile = profile.workspaceDir().resolve("prospects.csv");
        Path pendingDir = profile.workspaceDir().resolve("Pending");
        if (markdownFile.getParent() != null) {
            Files.createDirectories(markdownFile.getParent());
        }
        Files.createDirectories(pendingDir);

        if (!Files.exists(markdownFile)) {
            Files.writeString(markdownFile, "# Prospect List\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        }
        ensureCsvHeader(csvFile);
        normalizeCsvRows(csvFile);

        Set<String> existingKeys = readExistingKeys(csvFile);
        List<ProspectRecord> uniqueRecords = filterNewRecords(records, existingKeys);
        int skipped = records.size() - uniqueRecords.size();
        if (uniqueRecords.isEmpty()) {
            return "Saved 0 prospects. Skipped " + skipped + " duplicates already present in " + markdownFile;
        }

        StringBuilder markdown = new StringBuilder();
        markdown.append('\n');
        markdown.append("### Prospecting Run ").append(LocalDateTime.now().format(STAMP)).append('\n');
        markdown.append("- Search focus: ").append(searchFocus == null || searchFocus.isBlank() ? "(unspecified)" : searchFocus.trim()).append('\n');
        markdown.append("- Records added: ").append(uniqueRecords.size()).append('\n');
        markdown.append("- Duplicates skipped: ").append(skipped).append('\n').append('\n');
        for (ProspectRecord record : uniqueRecords) {
            markdown.append(record.toMarkdown()).append('\n').append('\n');
        }
        Files.writeString(markdownFile, markdown.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        StringBuilder csv = new StringBuilder();
        for (ProspectRecord record : uniqueRecords) {
            csv.append(record.toCsvRow()).append('\n');
        }
        Files.writeString(csvFile, csv.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        int pendingCreated = createPendingLetters(pendingDir, uniqueRecords);

        return "Saved " + uniqueRecords.size() + " prospects to " + markdownFile + " and " + csvFile
                + ". Created " + pendingCreated + " pending letters in " + pendingDir
                + ". Skipped " + skipped + " duplicates.";
    }

    private int createPendingLetters(Path pendingDir, List<ProspectRecord> records) throws IOException {
        int created = 0;
        for (ProspectRecord record : records) {
            Path file = pendingDir.resolve(pendingFileName(record));
            if (Files.exists(file)) {
                continue;
            }
            Files.writeString(
                    file,
                    renderPendingLetter(record),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            created++;
        }
        return created;
    }

    private String pendingFileName(ProspectRecord record) {
        String slug = slugify(record.company());
        if (slug.isBlank()) {
            slug = "prospect";
        }
        return slug + ".md";
    }

    private String renderPendingLetter(ProspectRecord record) {
        String company = valueOrDefault(record.company(), "Prospect");
        String contactName = valueOrDefault(record.contactName(), "");
        String contactRole = valueOrDefault(record.contactRole(), "Owner / Production Manager");
        String contactEmail = valueOrDefault(record.contactEmail(), "");
        String website = valueOrDefault(record.website(), "");
        String location = valueOrDefault(record.location(), "");
        String whyFit = valueOrDefault(record.whyFit(), "");
        String evidence = valueOrDefault(record.evidence(), "");
        String greeting = contactName.isBlank() ? "Hi" : "Hi " + contactName;
        String subject = "Arma Automotive tube notching workflow for " + company;

        StringBuilder out = new StringBuilder();
        out.append("# Pending Outreach Draft: ").append(company).append('\n').append('\n');
        out.append("- Status: pending review\n");
        out.append("- Subject: ").append(subject).append('\n');
        out.append("- To: ").append(contactEmail).append('\n');
        out.append("- Contact: ").append(contactName).append('\n');
        out.append("- Role: ").append(contactRole).append('\n');
        out.append("- Company: ").append(company).append('\n');
        out.append("- Website: ").append(website).append('\n');
        out.append("- Location: ").append(location).append('\n');
        out.append("- Fit score: ").append(valueOrDefault(record.fitScore(), "")).append('\n');
        out.append("- Why fit: ").append(whyFit).append('\n');
        out.append("- Evidence: ").append(evidence).append('\n');
        if (record.sourceUrls() != null && !record.sourceUrls().isEmpty()) {
            out.append("- Sources: ").append(String.join(", ", record.sourceUrls())).append('\n');
        }
        out.append('\n');
        out.append("## Draft\n\n");
        out.append(greeting).append(",\n\n");
        out.append("I came across ").append(company);
        if (!location.isBlank()) {
            out.append(" in ").append(location);
        }
        out.append(" and thought your team might be a fit for Arma Automotive's CNC tube notcher.\n\n");
        if (!whyFit.isBlank()) {
            out.append("From what I found, ").append(whyFit).append(' ').append('\n').append('\n');
        }
        if (!evidence.isBlank()) {
            out.append("The public signals that stood out were: ").append(evidence).append('\n').append('\n');
        }
        out.append("We built the CNC tube notcher for shops that need repeatable tube notching workflows with less manual setup and more consistent fit-up, especially for chassis, cages, and other tubular fabrication work.\n\n");
        out.append("If it would be useful, I can send a short overview of how shops are using it and whether it looks relevant for your workflow.\n\n");
        out.append("Best,\n");
        out.append("Arma Automotive\n");
        return out.toString().trim() + "\n";
    }

    private String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String lower = value.trim().toLowerCase();
        StringBuilder out = new StringBuilder(lower.length());
        boolean lastWasDash = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                out.append(c);
                lastWasDash = false;
            } else if (!lastWasDash) {
                out.append('-');
                lastWasDash = true;
            }
        }
        String slug = out.toString();
        while (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        while (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        return slug;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static List<ProspectRecord> filterNewRecords(List<ProspectRecord> records, Set<String> existingKeys) {
        List<ProspectRecord> unique = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>(existingKeys);
        for (ProspectRecord record : records) {
            List<String> keys = record.dedupeKeys();
            boolean duplicate = false;
            for (String key : keys) {
                if (seenKeys.contains(key)) {
                    duplicate = true;
                    break;
                }
            }
            if (duplicate) {
                continue;
            }
            unique.add(record);
            seenKeys.addAll(keys);
        }
        return unique;
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
            if (columns.size() < 12) {
                continue;
            }
            ProspectRecord record = columns.size() >= 19
                    ? new ProspectRecord(
                            columns.get(0),
                            columns.get(1),
                            columns.get(2),
                            columns.get(3),
                            columns.get(4),
                            columns.get(5),
                            columns.get(6),
                            columns.get(7),
                            columns.get(8),
                            columns.get(9),
                            columns.get(10),
                            columns.get(11),
                            columns.get(12),
                            columns.get(13),
                            splitSources(columns.get(14)),
                            columns.get(15),
                            columns.get(16),
                            splitSources(columns.get(17)),
                            columns.get(18))
                    : new ProspectRecord(
                            columns.get(0),
                            columns.get(1),
                            columns.get(2),
                            columns.get(3),
                            columns.get(4),
                            "",
                            columns.get(5),
                            columns.get(6),
                            columns.get(7),
                            "new",
                            "medium",
                            "unassigned",
                            "",
                            "",
                            List.of(),
                            columns.get(8),
                            columns.get(9),
                            splitSources(columns.get(10)),
                            columns.get(11));
            keys.addAll(record.dedupeKeys());
        }
        return keys;
    }

    private static void ensureCsvHeader(Path csvFile) throws IOException {
        if (!Files.exists(csvFile)) {
            Files.writeString(csvFile, CSV_HEADER, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            return;
        }
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            Files.writeString(csvFile, CSV_HEADER, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return;
        }
        if ((lines.get(0) + "\n").equals(CSV_HEADER)) {
            return;
        }
        if (LEGACY_CSV_HEADER.equals(lines.get(0).trim())) {
            migrateLegacyCsv(csvFile, lines);
            return;
        }
        lines.set(0, CSV_HEADER.trim());
        Files.write(csvFile, lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private static void migrateLegacyCsv(Path csvFile, List<String> lines) throws IOException {
        List<String> rewritten = new ArrayList<>();
        rewritten.add(CSV_HEADER.trim());
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            List<String> columns = parseCsvLine(line);
            if (columns.size() < 12) {
                continue;
            }
            ProspectRecord record = new ProspectRecord(
                    columns.get(0),
                    columns.get(1),
                    columns.get(2),
                    columns.get(3),
                    columns.get(4),
                    "",
                    columns.get(5),
                    columns.get(6),
                    columns.get(7),
                    "new",
                    "medium",
                    "unassigned",
                    "",
                    "",
                    List.of(),
                    columns.get(8),
                    columns.get(9),
                    splitSources(columns.get(10)),
                    columns.get(11));
            rewritten.add(record.toCsvRow());
        }
        Files.write(csvFile, rewritten, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private static void normalizeCsvRows(Path csvFile) throws IOException {
        if (!Files.exists(csvFile)) {
            return;
        }
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return;
        }
        List<String> rewritten = new ArrayList<>();
        rewritten.add(CSV_HEADER.trim());
        boolean changed = false;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            List<String> columns = parseCsvLine(line);
            if (columns.size() == 19) {
                rewritten.add(line);
                continue;
            }
            if (columns.size() >= 12) {
                ProspectRecord record = new ProspectRecord(
                        columns.get(0),
                        columns.get(1),
                        columns.get(2),
                        columns.get(3),
                        columns.get(4),
                        "",
                        columns.get(5),
                        columns.get(6),
                        columns.get(7),
                        "new",
                        "medium",
                        "unassigned",
                        "",
                        "",
                        List.of(),
                        columns.get(8),
                        columns.get(9),
                        splitSources(columns.get(10)),
                        columns.get(11));
                rewritten.add(record.toCsvRow());
                changed = true;
            } else {
                changed = true;
            }
        }
        if (changed) {
            Files.write(csvFile, rewritten, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
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

    private static List<String> splitSources(String value) {
        List<String> sources = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return sources;
        }
        String[] parts = value.split("\\|");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                sources.add(trimmed);
            }
        }
        return sources;
    }
}
