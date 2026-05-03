package helion;

import java.util.ArrayList;
import java.util.List;

public final class ProviderFactory {
    private ProviderFactory() {
    }

    public static BusinessAgent createAgent(HelionConfig config) {
        UsageTracker usageTracker = createUsageTracker(config);
        return new BusinessAgent(
                createManager(config, usageTracker),
                createWorkers(config, usageTracker),
                createBrowserTool(config),
                createKnowledgeBase(config),
                createCompanyDataCorpus(config),
                createCompanyDataSources(config),
                createAgentRegistry(config),
                usageTracker,
                createMemoryStore(config),
                createEmailDraftStore(config),
                config);
    }

    public static LlmProvider createManager(HelionConfig config, UsageTracker usageTracker) {
        return createProvider(config.managerProvider(), config.managerModel(), config, usageTracker);
    }

    public static WorkerPool createWorkers(HelionConfig config, UsageTracker usageTracker) {
        List<LlmProvider> workers = new ArrayList<>();
        int count = Math.max(1, config.workerCount());
        for (int i = 0; i < count; i++) {
            workers.add(createProvider(config.workerProvider(), config.workerModel(), config, usageTracker));
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

    public static KnowledgeBase createKnowledgeBase(HelionConfig config) {
        return new KnowledgeBase(
                config.knowledgeEnabled(),
                config.knowledgeDir(),
                config.knowledgeCharLimit());
    }

    public static MultiDirectoryCorpus createCompanyDataCorpus(HelionConfig config) {
        return new MultiDirectoryCorpus(
                createCompanyDataSources(config),
                config.companyDataDir(),
                config.companyDataCharLimit(),
                4);
    }

    public static CompanyDataSources createCompanyDataSources(HelionConfig config) {
        return new CompanyDataSources(config.companyDataSourcesFile());
    }

    public static AgentRegistry createAgentRegistry(HelionConfig config) {
        return new AgentRegistry(config.agentsDir());
    }

    public static EmailDraftStore createEmailDraftStore(HelionConfig config) {
        return new EmailDraftStore(config.emailSettings(), createAgentRegistry(config));
    }

    public static UsageTracker createUsageTracker(HelionConfig config) {
        return new UsageTracker(config.usageEventsFile());
    }

    private static LlmProvider createProvider(String providerName, String model, HelionConfig config, UsageTracker usageTracker) {
        String provider = providerName == null ? "" : providerName.trim().toLowerCase();
        LlmProvider base;
        if ("openai".equals(provider) && !config.openAiApiKey().isBlank()) {
            base = new OpenAiProvider(config.openAiApiKey(), model);
        } else if ("llama.cpp".equals(provider) || "llamacpp".equals(provider) || "llama".equals(provider)) {
            base = new LlamaCppProvider(config.llamaCppBaseUrl(), model);
        } else {
            base = new DemoBusinessProvider();
        }
        return new UsageTrackingProvider(base, usageTracker);
    }
}
