package helion;

public record UsageMetrics(
        String provider,
        String model,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        boolean exact) {
}
