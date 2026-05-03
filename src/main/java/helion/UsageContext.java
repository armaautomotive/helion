package helion;

import java.util.concurrent.Callable;

public final class UsageContext {
    private static final ThreadLocal<String> CURRENT_AGENT_ID = new ThreadLocal<>();

    private UsageContext() {
    }

    public static String currentAgentId() {
        String value = CURRENT_AGENT_ID.get();
        return value == null ? "" : value;
    }

    public static <T> T withAgent(String agentId, Callable<T> action) throws Exception {
        String previous = CURRENT_AGENT_ID.get();
        set(agentId);
        try {
            return action.call();
        } finally {
            restore(previous);
        }
    }

    private static void set(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            CURRENT_AGENT_ID.remove();
            return;
        }
        CURRENT_AGENT_ID.set(agentId.trim());
    }

    private static void restore(String previous) {
        if (previous == null || previous.isBlank()) {
            CURRENT_AGENT_ID.remove();
            return;
        }
        CURRENT_AGENT_ID.set(previous);
    }
}
