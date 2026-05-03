package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MemoryStore {
    private final boolean enabled;
    private final Path baseDir;
    private final String namespace;
    private final int maxEntries;
    private final int maxCharsPerEntry;

    public MemoryStore(boolean enabled, Path baseDir, String namespace, int maxEntries, int maxCharsPerEntry) {
        this.enabled = enabled;
        this.baseDir = baseDir;
        this.namespace = sanitize(namespace.isBlank() ? "default" : namespace);
        this.maxEntries = Math.max(1, maxEntries);
        this.maxCharsPerEntry = Math.max(200, maxCharsPerEntry);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String describe() {
        if (!enabled) {
            return "disabled";
        }
        return "enabled namespace=" + namespace + " dir=" + baseDir;
    }

    public List<String> listKeys() throws IOException {
        if (!enabled) {
            return List.of();
        }
        Path dir = namespaceDir();
        if (!Files.exists(dir)) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        if (name.endsWith(".md")) {
                            keys.add(name.substring(0, name.length() - 3));
                        }
                    });
        }
        return keys;
    }

    public String read(String key) throws IOException {
        if (!enabled) {
            return "Memory is disabled.";
        }
        Path file = fileForKey(key);
        if (!Files.exists(file)) {
            return "No memory found for key: " + sanitize(key);
        }
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    public void append(String key, String content) throws IOException {
        if (!enabled) {
            return;
        }
        Files.createDirectories(namespaceDir());
        Path file = fileForKey(key);
        String entry = formatEntry(content);
        Files.writeString(file, entry, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        compact(file);
    }

    private String formatEntry(String content) {
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        return "## " + timestamp + System.lineSeparator()
                + TextUtils.limit(content.trim(), maxCharsPerEntry) + System.lineSeparator()
                + System.lineSeparator();
    }

    private void compact(Path file) throws IOException {
        String raw = Files.readString(file, StandardCharsets.UTF_8);
        String[] entries = raw.split("(?m)^## ");
        List<String> kept = new ArrayList<>();
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                kept.add("## " + trimmed + System.lineSeparator() + System.lineSeparator());
            }
        }
        if (kept.size() <= maxEntries) {
            return;
        }
        List<String> tail = kept.subList(Math.max(0, kept.size() - maxEntries), kept.size());
        Files.writeString(file, String.join("", tail), StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    private Path namespaceDir() {
        return baseDir.resolve(namespace);
    }

    private Path fileForKey(String key) {
        return namespaceDir().resolve(sanitize(key) + ".md");
    }

    private static String sanitize(String value) {
        String input = value == null ? "default" : value.trim().toLowerCase();
        StringBuilder out = new StringBuilder(input.length());
        boolean lastDash = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            boolean allowed = (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '.'
                    || c == '_'
                    || c == '-';
            if (allowed) {
                if (c == '-') {
                    if (!lastDash && out.length() > 0) {
                        out.append(c);
                    }
                    lastDash = true;
                } else {
                    out.append(c);
                    lastDash = false;
                }
            } else if (!lastDash && out.length() > 0) {
                out.append('-');
                lastDash = true;
            }
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '-') {
            out.setLength(out.length() - 1);
        }
        String cleaned = out.toString();
        return cleaned.isBlank() ? "default" : cleaned;
    }
}
