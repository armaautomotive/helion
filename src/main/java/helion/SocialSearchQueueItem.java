package helion;

import java.time.LocalDateTime;

public record SocialSearchQueueItem(
        int id,
        String site,
        String topic,
        String audience,
        String queryTemplate,
        int pass,
        String status,
        LocalDateTime lastRun,
        LocalDateTime nextRun,
        int resultsSeen,
        String notes) {

    public String effectiveQuery() {
        StringBuilder query = new StringBuilder();
        if (site != null && !site.isBlank()) {
            query.append("site:").append(site.trim());
        }
        append(query, queryTemplate);
        append(query, topic);
        append(query, audience);
        String suffix = switch (Math.max(1, pass)) {
            case 1 -> "discussion OR question OR forum";
            case 2 -> "\"tube notcher\" OR \"tube notching\" OR cope";
            case 3 -> "\"roll cage\" OR \"tube chassis\" OR fabrication";
            case 4 -> "\"looking for\" OR recommend OR need";
            default -> "problem OR workflow";
        };
        append(query, suffix);
        return query.toString().trim();
    }

    public SocialSearchQueueItem afterRun(int newResultsSeen, int delaySeconds) {
        int currentPass = Math.max(1, pass);
        int nextPass = currentPass >= 4 ? 1 : currentPass + 1;
        LocalDateTime now = LocalDateTime.now();
        return new SocialSearchQueueItem(
                id,
                site,
                topic,
                audience,
                queryTemplate,
                nextPass,
                "active",
                now,
                now.plusSeconds(Math.max(5, delaySeconds)),
                Math.max(0, newResultsSeen),
                notes);
    }

    public boolean isDue(LocalDateTime now) {
        if ("paused".equalsIgnoreCase(status) || "done".equalsIgnoreCase(status)) {
            return false;
        }
        return nextRun == null || !nextRun.isAfter(now);
    }

    public String toCsvRow() {
        return String.join(",",
                csv(Integer.toString(id)),
                csv(site),
                csv(topic),
                csv(audience),
                csv(queryTemplate),
                csv(Integer.toString(pass)),
                csv(status),
                csv(formatDateTime(lastRun)),
                csv(formatDateTime(nextRun)),
                csv(Integer.toString(resultsSeen)),
                csv(notes));
    }

    public String summaryLine() {
        return id + ". [" + status + "] pass " + pass + " next " + (nextRun == null ? "(now)" : nextRun)
                + " | " + (site == null || site.isBlank() ? "(any site)" : site) + " | " + effectiveQuery();
    }

    private static void append(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(value.trim());
    }

    private static String csv(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private static String formatDateTime(LocalDateTime value) {
        if (value == null || LocalDateTime.MIN.equals(value)) {
            return "";
        }
        return value.toString();
    }
}
