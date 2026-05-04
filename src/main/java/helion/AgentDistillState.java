package helion;

import java.time.LocalDateTime;

public record AgentDistillState(
        LocalDateTime lastCheckedAt,
        LocalDateTime lastDistilledAt,
        LocalDateTime latestSourceModifiedAt,
        String providerName,
        String providerModel) {

    public static AgentDistillState empty() {
        return new AgentDistillState(null, null, null, "", "");
    }
}
