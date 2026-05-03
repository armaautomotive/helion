package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public final class AgentStatusStore {
    private final HelionConfig config;

    public AgentStatusStore(HelionConfig config) {
        this.config = config;
    }

    public AgentStatus read(AgentProfile profile) throws IOException {
        return AgentStatus.parse(readIfExists(profile.statusFile()), config);
    }

    public void markRun(AgentProfile profile, LocalDateTime when) throws IOException {
        AgentStatus status = read(profile);
        Path file = profile.statusFile();
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(
                file,
                status.withLastRunText(when),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    public void updateSettings(
            AgentProfile profile,
            String runState,
            String executionTarget,
            String preferredLocalPool,
            int runIntervalSeconds,
            String primaryOutputFile) throws IOException {
        AgentStatus status = read(profile);
        Path file = profile.statusFile();
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(
                file,
                status.withSettingsText(runState, executionTarget, preferredLocalPool, runIntervalSeconds, primaryOutputFile),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private static String readIfExists(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return "";
        }
        return Files.readString(path, StandardCharsets.UTF_8).trim();
    }
}
