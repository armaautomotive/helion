package helion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Properties;

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
        boolean knowledgeEnabled,
        Path knowledgeDir,
        int knowledgeCharLimit,
        Path agentsDir,
        Path companyDataDir,
        Path companyDataSourcesFile,
        int companyDataCharLimit,
        String webHost,
        int webPort,
        Path usageEventsFile,
        EmailSettings emailSettings,
        String openAiApiKey,
        String llamaCppBaseUrl) {
    private static final String DEFAULT_MANAGER_PROVIDER = "demo";
    private static final String DEFAULT_WORKER_PROVIDER = "llama.cpp";
    private static final String DEFAULT_OPENAI_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_LLAMACPP_MODEL = "local-model";
    private static final String DEFAULT_CONFIG_FILE = "helion.properties";

    public static HelionConfig load() {
        Properties properties = loadProperties();
        String managerProvider = config(properties, "HELION_MANAGER_PROVIDER", "helion.manager.provider",
                config(properties, "HELION_PROVIDER", "helion.provider", DEFAULT_MANAGER_PROVIDER)).toLowerCase();
        String managerModel = config(properties, "HELION_MANAGER_MODEL", "helion.manager.model", DEFAULT_OPENAI_MODEL);
        String workerProvider = config(properties, "HELION_WORKER_PROVIDER", "helion.worker.provider", DEFAULT_WORKER_PROVIDER).toLowerCase();
        String workerModel = config(properties, "HELION_WORKER_MODEL", "helion.worker.model", DEFAULT_LLAMACPP_MODEL);
        int workerCount = intConfig(properties, "HELION_WORKER_COUNT", "helion.worker.count", 2);
        int maxTurns = intConfig(properties, "HELION_MAX_TURNS", "helion.max.turns", 6);
        boolean browserEnabled = boolConfig(properties, "HELION_ENABLE_BROWSER", "helion.enable.browser", false);
        int browserResultLimit = intConfig(properties, "HELION_BROWSER_RESULT_LIMIT", "helion.browser.result_limit", 5);
        int browserFetchCharLimit = intConfig(properties, "HELION_BROWSER_FETCH_CHAR_LIMIT", "helion.browser.fetch_char_limit", 4000);
        boolean memoryEnabled = boolConfig(properties, "HELION_ENABLE_MEMORY", "helion.enable.memory", true);
        Path memoryDir = Path.of(config(properties, "HELION_MEMORY_DIR", "helion.memory.dir", ".helion/memory"));
        String memoryNamespace = config(properties, "HELION_MEMORY_NAMESPACE", "helion.memory.namespace", "default");
        int memoryMaxEntries = intConfig(properties, "HELION_MEMORY_MAX_ENTRIES", "helion.memory.max_entries", 24);
        int memoryEntryCharLimit = intConfig(properties, "HELION_MEMORY_ENTRY_CHAR_LIMIT", "helion.memory.entry_char_limit", 2500);
        boolean knowledgeEnabled = boolConfig(properties, "HELION_ENABLE_KNOWLEDGE", "helion.enable.knowledge", true);
        Path knowledgeDir = Path.of(config(properties, "HELION_KNOWLEDGE_DIR", "helion.knowledge.dir", "knowledge"));
        int knowledgeCharLimit = intConfig(properties, "HELION_KNOWLEDGE_CHAR_LIMIT", "helion.knowledge.char_limit", 12000);
        Path agentsDir = Path.of(config(properties, "HELION_AGENTS_DIR", "helion.agents.dir", "agents"));
        Path companyDataDir = Path.of(config(properties, "HELION_COMPANY_DATA_DIR", "helion.company_data.dir", "company_data"));
        Path companyDataSourcesFile = Path.of(config(properties, "HELION_COMPANY_DATA_SOURCES_FILE", "helion.company_data.sources_file", ".helion/company_data_sources.txt"));
        int companyDataCharLimit = intConfig(properties, "HELION_COMPANY_DATA_CHAR_LIMIT", "helion.company_data.char_limit", 12000);
        String webHost = config(properties, "HELION_WEB_HOST", "helion.web.host", "127.0.0.1");
        int webPort = intConfig(properties, "HELION_WEB_PORT", "helion.web.port", 8421);
        Path usageEventsFile = Path.of(config(properties, "HELION_USAGE_EVENTS_FILE", "helion.usage.events_file", ".helion/usage_events.tsv"));
        EmailSettings emailSettings = new EmailSettings(
                boolConfig(properties, "HELION_EMAIL_ENABLED", "helion.email.enabled", false),
                config(properties, "HELION_EMAIL_PROVIDER", "helion.email.provider", "imap_smtp"),
                config(properties, "HELION_EMAIL_DISPLAY_NAME", "helion.email.display_name", ""),
                config(properties, "HELION_EMAIL_ADDRESS", "helion.email.address", ""),
                config(properties, "HELION_EMAIL_IMAP_HOST", "helion.email.imap.host", ""),
                intConfig(properties, "HELION_EMAIL_IMAP_PORT", "helion.email.imap.port", 993),
                config(properties, "HELION_EMAIL_IMAP_USERNAME", "helion.email.imap.username", ""),
                config(properties, "HELION_EMAIL_IMAP_PASSWORD", "helion.email.imap.password", ""),
                boolConfig(properties, "HELION_EMAIL_IMAP_SSL", "helion.email.imap.ssl", true),
                config(properties, "HELION_EMAIL_SMTP_HOST", "helion.email.smtp.host", ""),
                intConfig(properties, "HELION_EMAIL_SMTP_PORT", "helion.email.smtp.port", 465),
                config(properties, "HELION_EMAIL_SMTP_USERNAME", "helion.email.smtp.username", ""),
                config(properties, "HELION_EMAIL_SMTP_PASSWORD", "helion.email.smtp.password", ""),
                boolConfig(properties, "HELION_EMAIL_SMTP_SSL", "helion.email.smtp.ssl", true));
        String apiKey = config(properties, "OPENAI_API_KEY", "helion.openai.api_key", "");
        String llamaCppBaseUrl = config(properties, "HELION_LLAMACPP_URL", "helion.llamacpp.url", "http://localhost:8080");
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
                knowledgeEnabled,
                knowledgeDir,
                knowledgeCharLimit,
                agentsDir,
                companyDataDir,
                companyDataSourcesFile,
                companyDataCharLimit,
                webHost,
                webPort,
                usageEventsFile,
                emailSettings,
                apiKey,
                llamaCppBaseUrl);
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        Path configPath = Path.of(System.getProperty("user.dir")).resolve(DEFAULT_CONFIG_FILE);
        if (!Files.exists(configPath)) {
            return properties;
        }
        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load config file: " + configPath, ex);
        }
        return properties;
    }

    private static String config(Properties properties, String envKey, String propertyKey, String fallback) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        String propertyValue = properties.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        return fallback;
    }

    private static int intConfig(Properties properties, String envKey, String propertyKey, int fallback) {
        try {
            return Integer.parseInt(config(properties, envKey, propertyKey, Integer.toString(fallback)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static boolean boolConfig(Properties properties, String envKey, String propertyKey, boolean fallback) {
        String value = config(properties, envKey, propertyKey, Boolean.toString(fallback));
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }
}
