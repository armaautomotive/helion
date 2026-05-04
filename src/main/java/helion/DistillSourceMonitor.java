package helion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class DistillSourceMonitor {
    private final HelionConfig config;
    private final CompanyDataSources companyDataSources;

    public DistillSourceMonitor(HelionConfig config, CompanyDataSources companyDataSources) {
        this.config = config;
        this.companyDataSources = companyDataSources;
    }

    public LocalDateTime latestModifiedAt(AgentProfile profile) throws IOException {
        LocalDateTime latest = null;
        latest = later(latest, modifiedAt(profile.roleFile()));
        latest = later(latest, modifiedAt(profile.distillFile()));
        latest = later(latest, modifiedAt(companyDataSources.sourcesFile()));

        if (config.knowledgeEnabled()) {
            latest = later(latest, latestInDirectory(config.knowledgeDir(), 3));
        }

        List<Path> companyDirs = new ArrayList<>(companyDataSources.list());
        if (companyDirs.isEmpty() && config.companyDataDir() != null) {
            companyDirs.add(config.companyDataDir());
        }
        for (Path dir : companyDirs) {
            latest = later(latest, latestInDirectory(dir, 4));
        }
        return latest;
    }

    private LocalDateTime latestInDirectory(Path dir, int maxDepth) throws IOException {
        if (dir == null || !Files.exists(dir)) {
            return null;
        }
        LocalDateTime latest = modifiedAt(dir);
        try (Stream<Path> stream = Files.walk(dir, maxDepth)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }
                latest = later(latest, modifiedAt(path));
            }
        }
        return latest;
    }

    private LocalDateTime modifiedAt(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return null;
        }
        FileTime fileTime = Files.getLastModifiedTime(path);
        return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
    }

    private LocalDateTime later(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }
}
