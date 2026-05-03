package helion;

public record UsageSummary(
        String provider,
        String model,
        long dailyPromptTokens,
        long dailyCompletionTokens,
        long dailyTotalTokens,
        long monthlyPromptTokens,
        long monthlyCompletionTokens,
        long monthlyTotalTokens,
        long yearlyPromptTokens,
        long yearlyCompletionTokens,
        long yearlyTotalTokens,
        long requests,
        boolean allExact) {
}
