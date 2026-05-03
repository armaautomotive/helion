package helion;

import java.time.LocalDate;

public record ProspectSearchQueueItem(
        int id,
        String queryTemplate,
        String region,
        String city,
        String industry,
        int pass,
        String status,
        LocalDate lastRun,
        LocalDate nextRun,
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

    public ProspectSearchQueueItem afterRun(int newResultsSeen) {
        int currentPass = Math.max(1, pass);
        int nextPass = currentPass >= 4 ? 1 : currentPass + 1;
        LocalDate today = LocalDate.now();
        LocalDate nextDate = currentPass >= 4 ? today.plusDays(30) : today.plusDays(1);
        String nextStatus = currentPass >= 4 ? "scheduled" : "active";
        return new ProspectSearchQueueItem(
                id,
                queryTemplate,
                region,
                city,
                industry,
                nextPass,
                nextStatus,
                today,
                nextDate,
                Math.max(0, newResultsSeen),
                notes);
    }

    public boolean isDue(LocalDate today) {
        if ("paused".equalsIgnoreCase(status) || "done".equalsIgnoreCase(status)) {
            return false;
        }
        return nextRun == null || !nextRun.isAfter(today);
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
                csv(lastRun == null ? "" : lastRun.toString()),
                csv(nextRun == null ? "" : nextRun.toString()),
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
}
