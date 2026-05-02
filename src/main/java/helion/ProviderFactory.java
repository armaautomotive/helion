package helion;

import java.util.ArrayList;
import java.util.List;

public final class ProviderFactory {
    private ProviderFactory() {
    }

    public static BusinessAgent createAgent(HelionConfig config) {
        return new BusinessAgent(
                createManager(config),
                createWorkers(config),
                createBrowserTool(config),
                createMemoryStore(config),
                config);
    }

    public static LlmProvider createManager(HelionConfig config) {
        return createProvider(config.managerProvider(), config.managerModel(), config);
    }

    public static WorkerPool createWorkers(HelionConfig config) {
        List<LlmProvider> workers = new ArrayList<>();
        int count = Math.max(1, config.workerCount());
        for (int i = 0; i < count; i++) {
            workers.add(createProvider(config.workerProvider(), config.workerModel(), config));
        }
        return new WorkerPool(workers);
    }

    public static BrowserTool createBrowserTool(HelionConfig config) {
        if (!config.browserEnabled()) {
            return new DisabledBrowserTool();
        }
        return new HttpBrowserTool(config.browserResultLimit(), config.browserFetchCharLimit());
    }

    public static MemoryStore createMemoryStore(HelionConfig config) {
        return new MemoryStore(
                config.memoryEnabled(),
                config.memoryDir(),
                config.memoryNamespace(),
                config.memoryMaxEntries(),
                config.memoryEntryCharLimit());
    }

    private static LlmProvider createProvider(String providerName, String model, HelionConfig config) {
        String provider = providerName == null ? "" : providerName.trim().toLowerCase();
        if ("openai".equals(provider) && !config.openAiApiKey().isBlank()) {
            return new OpenAiProvider(config.openAiApiKey(), model);
        }
        if ("llama.cpp".equals(provider) || "llamacpp".equals(provider) || "llama".equals(provider)) {
            return new LlamaCppProvider(config.llamaCppBaseUrl(), model);
        }
        return new DemoBusinessProvider();
    }
}
