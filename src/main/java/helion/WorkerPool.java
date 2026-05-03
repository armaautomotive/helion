package helion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public final class WorkerPool {
    private final Map<String, PoolEntry> pools;
    private final String defaultPoolName;

    public WorkerPool(List<PoolEntry> entries, String defaultPoolName) {
        this.pools = new LinkedHashMap<>();
        for (PoolEntry entry : entries) {
            this.pools.put(entry.name().toLowerCase(), entry);
        }
        this.defaultPoolName = normalizePoolName(defaultPoolName);
        if (!this.pools.containsKey(this.defaultPoolName) && !this.pools.isEmpty()) {
            String first = this.pools.keySet().iterator().next();
            this.pools.putIfAbsent(this.defaultPoolName, this.pools.get(first));
        }
    }

    public int size() {
        int total = 0;
        for (PoolEntry entry : pools.values()) {
            total += entry.capacity();
        }
        return total;
    }

    public List<String> poolNames() {
        return List.copyOf(pools.keySet());
    }

    public String defaultPoolName() {
        return defaultPoolName;
    }

    public String run(String title, String prompt, String preferredPool) throws IOException, InterruptedException {
        String systemPrompt = """
                You are a Helion worker.
                Complete the assigned subtask with concise analysis, evidence, and recommendations.
                Do not restate the full assignment unless it helps clarity.
                """;
        LlmProvider provider = providerForPool(preferredPool);
        String result = provider.complete(systemPrompt, prompt);
        return "[" + provider.name() + ":" + provider.modelName() + "] " + title + "\n" + result.trim();
    }

    public LlmProvider providerForPool(String preferredPool) {
        String normalizedPool = normalizePoolName(preferredPool);
        return new PooledProvider(normalizedPool);
    }

    private Lease acquire(String preferredPool) {
        String requested = normalizePoolName(preferredPool);
        PoolEntry preferred = pools.get(requested);
        if (preferred == null) {
            preferred = pools.get(defaultPoolName);
        }
        if (preferred == null) {
            throw new IllegalStateException("No local model pools configured.");
        }

        List<PoolEntry> ordered = new ArrayList<>(pools.values());
        ordered.sort(Comparator
                .comparingInt((PoolEntry entry) -> entry.availablePermits() <= 0 ? 1 : 0)
                .thenComparingInt(PoolEntry::activeJobs));
        ordered.remove(preferred);
        ordered.add(0, preferred);

        while (true) {
            for (PoolEntry entry : ordered) {
                if (entry.tryAcquire()) {
                    return new Lease(entry);
                }
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for a local model pool.", ex);
            }
        }
    }

    private static String normalizePoolName(String poolName) {
        return poolName == null || poolName.isBlank() ? "default" : poolName.trim().toLowerCase();
    }

    public static PoolEntry entry(String name, LlmProvider provider, int capacity) {
        return new PoolEntry(normalizePoolName(name), provider, Math.max(1, capacity));
    }

    private final class PooledProvider implements LlmProvider {
        private final String preferredPool;

        private PooledProvider(String preferredPool) {
            this.preferredPool = preferredPool;
        }

        @Override
        public String name() {
            PoolEntry entry = resolve(preferredPool);
            return entry.provider().name();
        }

        @Override
        public String modelName() {
            PoolEntry entry = resolve(preferredPool);
            return entry.provider().modelName();
        }

        @Override
        public LlmResult chatResult(List<LlmMessage> messages) throws IOException, InterruptedException {
            Lease lease = acquire(preferredPool);
            try {
                return lease.entry.provider().chatResult(messages);
            } finally {
                lease.entry.release();
            }
        }
    }

    private PoolEntry resolve(String preferredPool) {
        PoolEntry preferred = pools.get(normalizePoolName(preferredPool));
        if (preferred != null) {
            return preferred;
        }
        PoolEntry fallback = pools.get(defaultPoolName);
        if (fallback != null) {
            return fallback;
        }
        throw new IllegalStateException("No local model pools configured.");
    }

    private record Lease(PoolEntry entry) {
    }

    public static final class PoolEntry {
        private final String name;
        private final LlmProvider provider;
        private final int capacity;
        private final Semaphore permits;
        private final AtomicInteger activeJobs;

        private PoolEntry(String name, LlmProvider provider, int capacity) {
            this.name = name;
            this.provider = provider;
            this.capacity = capacity;
            this.permits = new Semaphore(capacity, true);
            this.activeJobs = new AtomicInteger(0);
        }

        public String name() {
            return name;
        }

        public LlmProvider provider() {
            return provider;
        }

        public int capacity() {
            return capacity;
        }

        public int activeJobs() {
            return activeJobs.get();
        }

        public int availablePermits() {
            return permits.availablePermits();
        }

        private boolean tryAcquire() {
            if (!permits.tryAcquire()) {
                return false;
            }
            activeJobs.incrementAndGet();
            return true;
        }

        private void release() {
            activeJobs.updateAndGet(value -> Math.max(0, value - 1));
            permits.release();
        }
    }
}
