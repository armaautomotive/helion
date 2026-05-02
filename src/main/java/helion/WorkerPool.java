package helion;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class WorkerPool {
    private final List<LlmProvider> workers;
    private final AtomicInteger cursor;

    public WorkerPool(List<LlmProvider> workers) {
        this.workers = List.copyOf(workers);
        this.cursor = new AtomicInteger(0);
    }

    public int size() {
        return workers.size();
    }

    public String run(String title, String prompt) throws IOException, InterruptedException {
        LlmProvider worker = nextWorker();
        String systemPrompt = """
                You are a Helion worker.
                Complete the assigned subtask with concise analysis, evidence, and recommendations.
                Do not restate the full assignment unless it helps clarity.
                """;
        String result = worker.complete(systemPrompt, prompt);
        return "[" + worker.name() + "] " + title + "\n" + result.trim();
    }

    private LlmProvider nextWorker() {
        int index = Math.floorMod(cursor.getAndIncrement(), workers.size());
        return workers.get(index);
    }
}
