package helion;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MultiDirectoryCorpus {
    private final CompanyDataSources sources;
    private final Path fallbackDir;
    private final int charLimit;
    private final int maxDepth;

    public MultiDirectoryCorpus(CompanyDataSources sources, Path fallbackDir, int charLimit, int maxDepth) {
        this.sources = sources;
        this.fallbackDir = fallbackDir;
        this.charLimit = Math.max(1000, charLimit);
        this.maxDepth = Math.max(1, maxDepth);
    }

    public String describe() {
        return "sources_file=" + sources.sourcesFile() + " fallback_dir=" + fallbackDir;
    }

    public String loadContext() throws IOException {
        List<Path> dirs = new ArrayList<>(sources.list());
        if (dirs.isEmpty() && fallbackDir != null) {
            dirs.add(fallbackDir);
        }
        if (dirs.isEmpty()) {
            return "No company data sources configured.";
        }

        StringBuilder out = new StringBuilder();
        for (Path dir : dirs) {
            DirectoryCorpus corpus = new DirectoryCorpus(true, dir, Math.max(2000, charLimit), maxDepth);
            String chunk = corpus.loadContext();
            if (chunk.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append("\n\n");
            }
            out.append("SOURCE_DIR: ").append(dir).append('\n');
            out.append(chunk);
            if (out.length() >= charLimit) {
                break;
            }
        }
        return TextUtils.limit(out.toString().trim(), charLimit);
    }
}
