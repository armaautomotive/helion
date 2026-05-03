package helion;

import java.nio.file.Path;

public record AgentProfile(
        String id,
        Path dir,
        Path roleFile,
        Path distillFile,
        Path statusFile,
        Path runtimeFile,
        Path distilledDir,
        Path workspaceDir) {
}
