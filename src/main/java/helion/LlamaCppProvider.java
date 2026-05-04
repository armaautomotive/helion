package helion;

public final class LlamaCppProvider extends OpenAiCompatibleProvider {
    public LlamaCppProvider(String baseUrl, String model, int requestTimeoutSeconds) {
        super("llama.cpp", normalize(baseUrl) + "/v1/chat/completions", model, null, requestTimeoutSeconds);
    }

    private static String normalize(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? "http://localhost:8080" : baseUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
