package helion;

import java.time.LocalDateTime;

public record ProspectSearchQueueItem(
        int id,
        String queryTemplate,
        String region,
        String city,
        String industry,
        int pass,
        String status,
        LocalDateTime lastRun,
        LocalDateTime nextRun,
        int resultsSeen,
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

    public ProspectSearchQueueItem afterRun(int newResultsSeen, int delaySeconds) {
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
                nextPass,
                nextStatus,
                now,
                nextDate,
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
                csv(queryTemplate),
                csv(region),
                csv(city),
                csv(industry),
                csv(Integer.toString(pass)),
                csv(status),
                csv(formatDateTime(lastRun)),
                csv(formatDateTime(nextRun)),
                csv(Integer.toString(resultsSeen)),
                csv(notes));
    }

    public String summaryLine() {
        return id + ". [" + status + "] pass " + pass + " next " + (nextRun == null ? "(now)" : nextRun) + " | " + effectiveQuery();
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
