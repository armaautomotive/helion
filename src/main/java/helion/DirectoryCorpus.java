package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class DirectoryCorpus {
    private final boolean enabled;
    private final Path dir;
    private final int charLimit;
    private final int maxDepth;

    public DirectoryCorpus(boolean enabled, Path dir, int charLimit, int maxDepth) {
        this.enabled = enabled;
        this.dir = dir;
        this.charLimit = Math.max(1000, charLimit);
        this.maxDepth = Math.max(1, maxDepth);
    }

    public String describe() {
        if (!enabled) {
            return "disabled";
        }
        return "enabled dir=" + dir;
    }

    public String loadContext() throws IOException {
        if (!enabled) {
            return "Directory corpus disabled.";
        }
        if (!Files.exists(dir)) {
            return "Directory not found: " + dir;
        }

        List<Path> files = listFiles();
        if (files.isEmpty()) {
            return "No readable corpus files found in " + dir;
        }

        StringBuilder out = new StringBuilder();
        for (Path file : files) {
            String relative = dir.relativize(file).toString();
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

    private List<Path> listFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(dir, maxDepth)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::isTextLike)
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(files::add);
        }
        return files;
    }

    private boolean isTextLike(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".md") || name.endsWith(".txt") || name.endsWith(".csv");
    }
}
