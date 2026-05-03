package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class KnowledgeBase {
    private final boolean enabled;
    private final Path knowledgeDir;
    private final int charLimit;

    public KnowledgeBase(boolean enabled, Path knowledgeDir, int charLimit) {
        this.enabled = enabled;
        this.knowledgeDir = knowledgeDir;
        this.charLimit = Math.max(1000, charLimit);
    }

    public String describe() {
        if (!enabled) {
            return "disabled";
        }
        return "enabled dir=" + knowledgeDir;
    }

    public String loadContext() throws IOException {
        if (!enabled) {
            return "Knowledge base disabled.";
        }
        if (!Files.exists(knowledgeDir)) {
            return "Knowledge directory not found: " + knowledgeDir;
        }

        List<Path> files = listKnowledgeFiles();
        if (files.isEmpty()) {
            return "No knowledge files found in " + knowledgeDir;
        }

        StringBuilder out = new StringBuilder();
        for (Path file : files) {
            String relative = knowledgeDir.relativize(file).toString();
            String content = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (content.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append("\n\n");
            }
            out.append("FILE: ").append(relative).append('\n');
            out.append(TextUtils.limit(content, 3000));
            if (out.length() >= charLimit) {
                break;
            }
        }
        return TextUtils.limit(out.toString().trim(), charLimit);
    }

    private List<Path> listKnowledgeFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(knowledgeDir, 3)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::isKnowledgeFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(files::add);
        }
        return files;
    }

    private boolean isKnowledgeFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".md") || name.endsWith(".txt");
    }
}
