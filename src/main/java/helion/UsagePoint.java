package helion;

public record UsagePoint(
        String label,
        long totalTokens,
        long requests) {
}
