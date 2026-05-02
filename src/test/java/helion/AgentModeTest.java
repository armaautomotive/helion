package helion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AgentModeTest {
    @Test
    void parsesKnownModes() {
        assertEquals(AgentMode.PLAN, AgentMode.fromCliArg("plan"));
        assertEquals(AgentMode.ANALYZE, AgentMode.fromCliArg("analysis"));
        assertEquals(AgentMode.EMAIL, AgentMode.fromCliArg("email"));
        assertEquals(AgentMode.GENERAL, AgentMode.fromCliArg("chat"));
    }

    @Test
    void identifiesModeTokens() {
        assertTrue(AgentMode.looksLikeMode("plan"));
        assertTrue(AgentMode.looksLikeMode("outreach"));
    }
}
