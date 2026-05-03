package helion;

public record LocalModelConfig(
        String poolName,
        String provider,
        String model,
        String baseUrl,
        int capacity) {

    public LocalModelConfig {
        poolName = poolName == null || poolName.isBlank() ? "default" : poolName.trim();
        provider = provider == null || provider.isBlank() ? "llama.cpp" : provider.trim();
        model = model == null ? "" : model.trim();
        baseUrl = baseUrl == null ? "" : baseUrl.trim();
        capacity = Math.max(1, capacity);
    }
}
