package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CompanyDataSources {
    private final Path sourcesFile;

    public CompanyDataSources(Path sourcesFile) {
        this.sourcesFile = sourcesFile;
    }

    public Path sourcesFile() {
        return sourcesFile;
    }

    public List<Path> list() throws IOException {
        if (!Files.exists(sourcesFile)) {
            return List.of();
        }
        List<Path> paths = new ArrayList<>();
        for (String line : Files.readAllLines(sourcesFile, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            paths.add(Path.of(trimmed));
        }
        return paths;
    }

    public void add(Path path) throws IOException {
        List<Path> current = new ArrayList<>(list());
        Path normalized = normalize(path);
        if (!contains(current, normalized)) {
            current.add(normalized);
            save(current);
        }
    }

    public void update(int index, Path path) throws IOException {
        List<Path> current = new ArrayList<>(list());
        checkIndex(current, index);
        current.set(index, normalize(path));
        save(current);
    }

    public void remove(int index) throws IOException {
        List<Path> current = new ArrayList<>(list());
        checkIndex(current, index);
        current.remove(index);
        save(current);
    }

    public String renderList() throws IOException {
        List<Path> paths = list();
        if (paths.isEmpty()) {
            return "No company data directories configured.";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            out.append(i + 1).append(". ").append(paths.get(i)).append('\n');
        }
        return out.toString().trim();
    }

    private void save(List<Path> paths) throws IOException {
        Files.createDirectories(sourcesFile.getParent());
        List<String> lines = new ArrayList<>();
        for (Path path : paths) {
            lines.add(path.toString());
        }
        Files.write(sourcesFile, lines, StandardCharsets.UTF_8);
    }

    private Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private boolean contains(List<Path> paths, Path candidate) {
        for (Path path : paths) {
            if (normalize(path).equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void checkIndex(List<Path> paths, int index) {
        if (index < 0 || index >= paths.size()) {
            throw new IllegalArgumentException("Invalid company data index: " + (index + 1));
        }
    }
}
