package helion;

public final class OpenAiProvider extends OpenAiCompatibleProvider {
    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    public OpenAiProvider(String apiKey, String model, int requestTimeoutSeconds) {
        super("openai", ENDPOINT, model, apiKey == null || apiKey.isBlank() ? null : "Bearer " + apiKey, requestTimeoutSeconds);
    }
}
