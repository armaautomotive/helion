package helion;

import java.nio.file.Path;

public record HelionConfig(
        String managerProvider,
        String managerModel,
        String workerProvider,
        String workerModel,
        int workerCount,
        int maxTurns,
        boolean browserEnabled,
        int browserResultLimit,
        int browserFetchCharLimit,
        boolean memoryEnabled,
        Path memoryDir,
        String memoryNamespace,
        int memoryMaxEntries,
        int memoryEntryCharLimit,
        String openAiApiKey,
        String llamaCppBaseUrl) {
    private static final String DEFAULT_MANAGER_PROVIDER = "demo";
    private static final String DEFAULT_WORKER_PROVIDER = "llama.cpp";
    private static final String DEFAULT_OPENAI_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_LLAMACPP_MODEL = "local-model";

    public static HelionConfig load() {
        String managerProvider = env("HELION_MANAGER_PROVIDER", env("HELION_PROVIDER", DEFAULT_MANAGER_PROVIDER)).toLowerCase();
        String managerModel = env("HELION_MANAGER_MODEL", DEFAULT_OPENAI_MODEL);
        String workerProvider = env("HELION_WORKER_PROVIDER", DEFAULT_WORKER_PROVIDER).toLowerCase();
        String workerModel = env("HELION_WORKER_MODEL", DEFAULT_LLAMACPP_MODEL);
        int workerCount = intEnv("HELION_WORKER_COUNT", 2);
        int maxTurns = intEnv("HELION_MAX_TURNS", 6);
        boolean browserEnabled = boolEnv("HELION_ENABLE_BROWSER", false);
        int browserResultLimit = intEnv("HELION_BROWSER_RESULT_LIMIT", 5);
        int browserFetchCharLimit = intEnv("HELION_BROWSER_FETCH_CHAR_LIMIT", 4000);
        boolean memoryEnabled = boolEnv("HELION_ENABLE_MEMORY", true);
        Path memoryDir = Path.of(env("HELION_MEMORY_DIR", ".helion/memory"));
        String memoryNamespace = env("HELION_MEMORY_NAMESPACE", "default");
        int memoryMaxEntries = intEnv("HELION_MEMORY_MAX_ENTRIES", 24);
        int memoryEntryCharLimit = intEnv("HELION_MEMORY_ENTRY_CHAR_LIMIT", 2500);
        String apiKey = env("OPENAI_API_KEY", "");
        String llamaCppBaseUrl = env("HELION_LLAMACPP_URL", "http://localhost:8080");
        return new HelionConfig(
                managerProvider,
                managerModel,
                workerProvider,
                workerModel,
                workerCount,
                maxTurns,
                browserEnabled,
                browserResultLimit,
                browserFetchCharLimit,
                memoryEnabled,
                memoryDir,
                memoryNamespace,
                memoryMaxEntries,
                memoryEntryCharLimit,
                apiKey,
                llamaCppBaseUrl);
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int intEnv(String key, int fallback) {
        try {
            return Integer.parseInt(env(key, Integer.toString(fallback)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static boolean boolEnv(String key, boolean fallback) {
        String value = env(key, Boolean.toString(fallback));
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }
}
