package helion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class AgentRegistry {
    private final Path agentsDir;

    public AgentRegistry(Path agentsDir) {
        this.agentsDir = agentsDir;
    }

    public String describe() {
        return agentsDir.toString();
    }

    public List<String> listAgentIds() throws IOException {
        if (!Files.exists(agentsDir)) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        try (Stream<Path> stream = Files.list(agentsDir)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> isVisibleAgentDirectory(path.getFileName().toString()))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> ids.add(path.getFileName().toString()));
        }
        return ids;
    }

    public AgentProfile load(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return null;
        }
        String id = agentId.trim();
        Path dir = agentsDir.resolve(id);
        if (!Files.isDirectory(dir)) {
            return null;
        }
        return new AgentProfile(
                id,
                dir,
                dir.resolve("role.md"),
                dir.resolve("distill.md"),
                dir.resolve("status.md"),
                dir.resolve("runtime.json"),
                dir.resolve("distilled"),
                dir.resolve("workspace"));
    }

    private static boolean isVisibleAgentDirectory(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return !name.startsWith(".") && !name.startsWith("_");
    }
}
