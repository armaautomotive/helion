package helion;

public record EmailInboxMessage(
        int sequenceNumber,
        String from,
        String subject,
        String date,
        String preview) {
}
