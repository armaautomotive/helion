package helion;

import java.time.LocalDateTime;

public record AgentActivityEntry(
        LocalDateTime timestamp,
        String level,
        String task,
        String summary,
        String details) {

    public String toJson() {
        return "{"
                + "\"timestamp\":\"" + TextUtils.escapeJson(timestamp == null ? "" : timestamp.toString()) + "\","
                + "\"level\":\"" + TextUtils.escapeJson(blank(level)) + "\","
                + "\"task\":\"" + TextUtils.escapeJson(blank(task)) + "\","
                + "\"summary\":\"" + TextUtils.escapeJson(blank(summary)) + "\","
                + "\"details\":\"" + TextUtils.escapeJson(blank(details)) + "\""
                + "}";
    }

    private static String blank(String value) {
        return value == null ? "" : value;
    }
}
