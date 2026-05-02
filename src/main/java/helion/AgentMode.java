package helion;

public enum AgentMode {
    PLAN,
    ANALYZE,
    EMAIL,
    GENERAL;

    public static AgentMode fromCliArg(String value) {
        if (value == null || value.isBlank()) {
            return GENERAL;
        }
        return switch (value.trim().toLowerCase()) {
            case "plan" -> PLAN;
            case "analyze", "analysis" -> ANALYZE;
            case "email", "outreach" -> EMAIL;
            case "general", "chat" -> GENERAL;
            default -> GENERAL;
        };
    }

    public static boolean looksLikeMode(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.equals("plan")
                || normalized.equals("analyze")
                || normalized.equals("analysis")
                || normalized.equals("email")
                || normalized.equals("outreach")
                || normalized.equals("general")
                || normalized.equals("chat");
    }
}
