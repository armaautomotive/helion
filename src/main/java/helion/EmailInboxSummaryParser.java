package helion;

import java.util.ArrayList;
import java.util.List;

public final class EmailInboxSummaryParser {
    private EmailInboxSummaryParser() {
    }

    public static List<EmailInboxSummaryMessage> parse(String markdown) {
        List<EmailInboxSummaryMessage> messages = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return messages;
        }

        int sequenceNumber = -1;
        String from = "";
        String subject = "";
        String date = "";
        String preview = "";
        boolean inMessage = false;

        for (String line : markdown.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("## Message ")) {
                if (inMessage && sequenceNumber >= 0) {
                    messages.add(new EmailInboxSummaryMessage(sequenceNumber, from, subject, date, preview));
                }
                sequenceNumber = parseSequence(trimmed.substring("## Message ".length()).trim());
                from = "";
                subject = "";
                date = "";
                preview = "";
                inMessage = true;
                continue;
            }
            if (!inMessage || trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("- From:")) {
                from = trimmed.substring("- From:".length()).trim();
            } else if (trimmed.startsWith("- Subject:")) {
                subject = trimmed.substring("- Subject:".length()).trim();
            } else if (trimmed.startsWith("- Date:")) {
                date = trimmed.substring("- Date:".length()).trim();
            } else if (trimmed.startsWith("- Preview:")) {
                preview = trimmed.substring("- Preview:".length()).trim();
            }
        }

        if (inMessage && sequenceNumber >= 0) {
            messages.add(new EmailInboxSummaryMessage(sequenceNumber, from, subject, date, preview));
        }
        return messages;
    }

    private static int parseSequence(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return -1;
        }
    }
}
