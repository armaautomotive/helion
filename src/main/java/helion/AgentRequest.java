package helion;

public record AgentRequest(AgentMode mode, String prompt, String agentId) {
    public AgentRequest(AgentMode mode, String prompt) {
        this(mode, prompt, "");
    }
}
