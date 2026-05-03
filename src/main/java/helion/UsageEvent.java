package helion;

import java.time.LocalDateTime;

public record UsageEvent(
        LocalDateTime timestamp,
        String provider,
        String model,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        boolean exact,
        String agentId) {
}
