package helion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AgentOutputResolver {
    private AgentOutputResolver() {
    }

    public static Path resolvePrimaryOutputFile(AgentProfile profile, HelionConfig config, String fallbackRelativePath) throws IOException {
        AgentStatus status = AgentStatus.parse(readIfExists(profile.statusFile()), config);
        String relative = status.primaryOutputFile();
        if (relative == null || relative.isBlank()) {
            relative = fallbackRelativePath;
        }
        return resolveWorkspaceFile(profile, relative);
    }

    public static Path resolveWorkspaceFile(AgentProfile profile, String relativePath) {
        String normalized = relativePath == null ? "" : relativePath.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("workspace/")) {
            normalized = normalized.substring("workspace/".length());
        }
        return profile.workspaceDir().resolve(normalized);
    }

    public static String relativeWorkspacePath(Path workspaceDir, Path file) {
        String relative = workspaceDir.relativize(file).toString().replace('\\', '/');
        return "workspace/" + relative;
    }

    private static String readIfExists(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return "";
        }
        return Files.readString(path).trim();
    }
}
