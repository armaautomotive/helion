package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AgentDistiller {
    private static final String GENERATED_MANIFEST = ".generated_files.txt";
    private static final List<String> PROSPECTING_FILES = List.of(
            "ideal_customer_profile.md",
            "pain_points.md",
            "buyer_signals.md",
            "disqualifiers.md",
            "product_claims.md",
            "sales_patterns.md");
    private final LlmProvider provider;
    private final KnowledgeBase knowledgeBase;
    private final MultiDirectoryCorpus companyDataCorpus;

    public AgentDistiller(LlmProvider provider, KnowledgeBase knowledgeBase, MultiDirectoryCorpus companyDataCorpus) {
        this.provider = provider;
        this.knowledgeBase = knowledgeBase;
        this.companyDataCorpus = companyDataCorpus;
    }

    public List<DistilledFile> distill(AgentProfile profile) throws IOException, InterruptedException {
        return distill(profile, provider);
    }

    public List<DistilledFile> distill(AgentProfile profile, LlmProvider selectedProvider) throws IOException, InterruptedException {
        String role = readIfExists(profile.roleFile());
        String distillInstructions = readIfExists(profile.distillFile());
        String knowledge = knowledgeBase.loadContext();
        String companyData = companyDataCorpus.loadContext();
        List<String> expectedFiles = expectedFilesFor(profile.id());
        LlmProvider activeProvider = selectedProvider == null ? provider : selectedProvider;

        if ("demo".equals(activeProvider.name())) {
            return buildDemoFiles(profile, role, distillInstructions, knowledge, companyData, expectedFiles);
        }

        String systemPrompt = """
                You are a distillation engine for a business agent.
                Convert broad internal business data into concise, role-specific markdown files.
                Follow the agent role and distillation instructions carefully.
                Output only file blocks in this exact format:

                FILE: filename.md
                CONTENT:
                markdown content
                <<<END_FILE>>>

                Rules:
                - Produce exactly the requested files.
                - Keep each file concise and factual.
                - Do not invent product claims or specs.
                - Prefer stable, reusable distilled knowledge over temporary notes.
                - Use the requested filenames exactly.
                - Do not output any text outside the file blocks.
                """;

        String userPrompt = """
                Agent ID:
                %s

                Agent role:
                %s

                Distillation instructions:
                %s

                Shared business knowledge:
                %s

                Company-wide source material:
                %s

                Required output files:
                %s
                """.formatted(
                profile.id(),
                defaultIfBlank(role, "No role.md content."),
                defaultIfBlank(distillInstructions, "No distill.md content."),
                defaultIfBlank(knowledge, "No shared knowledge loaded."),
                defaultIfBlank(companyData, "No company data loaded."),
                String.join(", ", expectedFiles));

        String raw = activeProvider.complete(systemPrompt, userPrompt);
        List<DistilledFile> files = canonicalizeFiles(parseFiles(raw), expectedFiles);
        if (files.isEmpty()) {
            files = buildFallbackFiles(raw, expectedFiles);
        }
        return files;
    }

    public void writeFiles(AgentProfile profile, List<DistilledFile> files) throws IOException {
        Files.createDirectories(profile.distilledDir());
        Set<String> previousGenerated = readGeneratedManifest(profile);
        Set<String> currentGenerated = new LinkedHashSet<>();
        for (DistilledFile file : files) {
            String normalizedName = normalizeDistilledFileName(file.name());
            currentGenerated.add(normalizedName);
            Path target = profile.distilledDir().resolve(normalizedName);
            Files.writeString(target, file.content().trim() + System.lineSeparator(), StandardCharsets.UTF_8);
        }
        cleanupObsoleteGeneratedFiles(profile, previousGenerated, currentGenerated);
        writeGeneratedManifest(profile, currentGenerated);
    }

    private List<DistilledFile> parseFiles(String raw) {
        List<DistilledFile> files = new ArrayList<>();
        String text = raw == null ? "" : raw.trim();
        int cursor = 0;
        while (cursor < text.length()) {
            int fileStart = text.indexOf("FILE:", cursor);
            if (fileStart < 0) {
                break;
            }
            int contentMarker = text.indexOf("CONTENT:", fileStart);
            if (contentMarker < 0) {
                break;
            }
            int fileNameStart = fileStart + "FILE:".length();
            String name = text.substring(fileNameStart, contentMarker).trim();
            int contentStart = contentMarker + "CONTENT:".length();
            int endMarker = text.indexOf("<<<END_FILE>>>", contentStart);
            if (endMarker < 0) {
                break;
            }
            String content = text.substring(contentStart, endMarker).trim();
            if (!name.isEmpty() && !content.isEmpty()) {
                files.add(new DistilledFile(name, content));
            }
            cursor = endMarker + "<<<END_FILE>>>".length();
        }
        return files;
    }

    private List<DistilledFile> buildDemoFiles(AgentProfile profile, String role, String distillInstructions, String knowledge, String companyData, List<String> expectedFiles) {
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("ideal_customer_profile.md", """
                # Ideal Customer Profile

                - Companies doing repeated tube fabrication or welded tube assembly work
                - Shops where throughput, repeatability, and fit-up quality affect profitability
                - Builders or manufacturers producing frames, racks, cages, chassis parts, or tube-based assemblies

                Source basis:
                %s
                """.formatted(TextUtils.limit(companyData, 1800)));
        sections.put("pain_points.md", """
                # Pain Points

                - Slow manual tube notching
                - Inconsistent notch quality between operators
                - Rework from poor fit-up
                - Bottlenecks in fabrication throughput
                - Difficulty scaling repeatable tube jobs

                Knowledge basis:
                %s
                """.formatted(TextUtils.limit(knowledge, 1800)));
        sections.put("buyer_signals.md", """
                # Buyer Signals

                - Hiring or growth in welding and fabrication
                - Expansion into higher-volume production
                - Public emphasis on shortening lead times
                - Evidence of repeated tube-assembly manufacturing
                - Need for more repeatable fabrication workflow

                Role / distill basis:
                %s
                """.formatted(TextUtils.limit(role + "\n\n" + distillInstructions, 1800)));
        sections.put("disqualifiers.md", """
                # Disqualifiers

                - No meaningful in-house fabrication
                - Little or no repeated tube work
                - Pure design or resale business with minimal manufacturing
                - One-off custom work where repeatability is not a strong value driver
                """);
        sections.put("product_claims.md", """
                # Product Claims

                Safe claims currently supported by local knowledge:

                - The CNC tube notcher is aimed at repeatable tube coping / notching work
                - It is relevant where fit-up quality, throughput, or setup time affects profitability
                - It is intended for fabrication environments where repeatability matters

                Shared knowledge:
                %s
                """.formatted(TextUtils.limit(knowledge, 1800)));
        sections.put("sales_patterns.md", """
                # Sales Patterns

                - Current business priority emphasizes CNC tube notcher sales
                - Revenue focus suggests near-term emphasis on practical fabrication-tool buyers
                - Product fit should be evaluated through operational need, not generic industry labels alone

                Company data basis:
                %s
                """.formatted(TextUtils.limit(companyData, 1800)));

        List<DistilledFile> files = new ArrayList<>();
        for (String fileName : expectedFiles) {
            String content = sections.get(fileName);
            if (content == null) {
                content = """
                        # Distilled Summary

                        Agent: %s

                        Role:
                        %s

                        Distillation priorities:
                        %s

                        Shared knowledge:
                        %s

                        Company data:
                        %s
                        """.formatted(
                        profile.id(),
                        TextUtils.limit(role, 1200),
                        TextUtils.limit(distillInstructions, 1200),
                        TextUtils.limit(knowledge, 1500),
                        TextUtils.limit(companyData, 1500));
            }
            files.add(new DistilledFile(fileName, content.trim()));
        }
        return files;
    }

    private String readIfExists(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return "";
        }
        return Files.readString(path, StandardCharsets.UTF_8).trim();
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<String> expectedFilesFor(String agentId) {
        if ("prospecting".equalsIgnoreCase(agentId)) {
            return PROSPECTING_FILES;
        }
        return List.of("distilled_summary.md");
    }

    private List<DistilledFile> canonicalizeFiles(List<DistilledFile> files, List<String> expectedFiles) {
        Map<String, DistilledFile> byName = new LinkedHashMap<>();
        for (DistilledFile file : files) {
            byName.put(normalizeDistilledFileName(file.name()), file);
        }
        List<DistilledFile> ordered = new ArrayList<>();
        for (String expected : expectedFiles) {
            DistilledFile exact = byName.get(expected);
            if (exact != null) {
                ordered.add(new DistilledFile(expected, exact.content()));
            }
        }
        if (!ordered.isEmpty()) {
            return ordered;
        }
        return files;
    }

    private List<DistilledFile> buildFallbackFiles(String raw, List<String> expectedFiles) {
        List<DistilledFile> files = new ArrayList<>();
        String summary = raw == null ? "" : raw.trim();
        for (String file : expectedFiles) {
            files.add(new DistilledFile(file, "# " + titleFromFile(file) + "\n\n" + summary));
        }
        return files;
    }

    private String titleFromFile(String fileName) {
        String base = fileName.endsWith(".md") ? fileName.substring(0, fileName.length() - 3) : fileName;
        String[] parts = base.split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.toString();
    }

    private String normalizeDistilledFileName(String name) {
        String raw = name == null ? "" : name.trim().replace('\\', '/');
        if (raw.isEmpty()) {
            return "distilled_summary.md";
        }
        int slash = raw.lastIndexOf('/');
        String base = slash >= 0 ? raw.substring(slash + 1) : raw;
        if (base.isBlank()) {
            base = "distilled_summary.md";
        }
        StringBuilder out = new StringBuilder(base.length());
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            boolean allowed = (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '.'
                    || c == '_'
                    || c == '-';
            out.append(allowed ? c : '_');
        }
        String normalized = out.toString();
        if (!normalized.toLowerCase().endsWith(".md")) {
            normalized = normalized + ".md";
        }
        return normalized;
    }

    private Set<String> readGeneratedManifest(AgentProfile profile) throws IOException {
        Set<String> files = new LinkedHashSet<>();
        Path manifest = profile.distilledDir().resolve(GENERATED_MANIFEST);
        if (Files.exists(manifest)) {
            for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    files.add(trimmed);
                }
            }
        }
        files.add("summary.md");
        files.add("distilled_summary.md");
        return files;
    }

    private void cleanupObsoleteGeneratedFiles(AgentProfile profile, Set<String> previousGenerated, Set<String> currentGenerated) throws IOException {
        for (String fileName : previousGenerated) {
            if (currentGenerated.contains(fileName) || GENERATED_MANIFEST.equals(fileName)) {
                continue;
            }
            Path target = profile.distilledDir().resolve(fileName);
            Files.deleteIfExists(target);
        }
    }

    private void writeGeneratedManifest(AgentProfile profile, Set<String> currentGenerated) throws IOException {
        Path manifest = profile.distilledDir().resolve(GENERATED_MANIFEST);
        List<String> lines = new ArrayList<>(currentGenerated);
        Files.write(manifest, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
