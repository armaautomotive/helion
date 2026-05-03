package helion;

public record EmailInboxSummaryMessage(
        int sequenceNumber,
        String from,
        String subject,
        String date,
        String preview) {
}
