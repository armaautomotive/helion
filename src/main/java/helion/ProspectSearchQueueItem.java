package helion;

import java.time.LocalDateTime;

public record ProspectSearchQueueItem(
        int id,
        String queryTemplate,
        String region,
        String city,
        String industry,
        int priority,
        int pass,
        String status,
        LocalDateTime lastRun,
        LocalDateTime nextRun,
        int attempts,
        int prospectsSaved,
        LocalDateTime lastSuccess,
        String notes) {

    public String effectiveQuery() {
        StringBuilder query = new StringBuilder();
        append(query, queryTemplate);
        append(query, city);
        append(query, region);
        append(query, industry);
        String suffix = switch (Math.max(1, pass)) {
            case 1 -> "fabrication shop";
            case 2 -> "contact OR owner OR production manager";
            case 3 -> "tube chassis OR roll cage OR welded tube";
            case 4 -> "email OR contact us";
            default -> "manufacturing";
        };
        append(query, suffix);
        return query.toString().trim();
    }

    public ProspectSearchQueueItem afterRun(int newProspectsSaved, int delaySeconds) {
        int currentPass = Math.max(1, pass);
        int nextPass = currentPass >= 4 ? 1 : currentPass + 1;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextDate = now.plusSeconds(Math.max(5, delaySeconds));
        String nextStatus = "active";
        return new ProspectSearchQueueItem(
                id,
                queryTemplate,
                region,
                city,
                industry,
                priority,
                nextPass,
                nextStatus,
                now,
                nextDate,
                Math.max(0, attempts + 1),
                Math.max(0, newProspectsSaved),
                newProspectsSaved > prospectsSaved ? now : lastSuccess,
                notes);
    }

    public double successRate() {
        if (attempts <= 0) {
            return 0.0;
        }
        return prospectsSaved / (double) attempts;
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
                csv(queryTemplate),
                csv(region),
                csv(city),
                csv(industry),
                csv(Integer.toString(priority)),
                csv(Integer.toString(pass)),
                csv(status),
                csv(formatDateTime(lastRun)),
                csv(formatDateTime(nextRun)),
                csv(Integer.toString(attempts)),
                csv(Integer.toString(prospectsSaved)),
                csv(formatDateTime(lastSuccess)),
                csv(notes));
    }

    public String summaryLine() {
        return id
                + ". [p" + priority + " " + status + "] pass " + pass
                + " next " + (nextRun == null ? "(now)" : nextRun)
                + " | attempts " + attempts
                + " | saved " + prospectsSaved
                + " | success " + Math.round(successRate() * 100) + "%"
                + " | last success " + (lastSuccess == null ? "(none)" : lastSuccess)
                + " | " + effectiveQuery();
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
