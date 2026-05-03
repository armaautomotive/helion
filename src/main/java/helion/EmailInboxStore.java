package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class EmailInboxStore {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EmailSettings settings;
    private final AgentRegistry agentRegistry;

    public EmailInboxStore(EmailSettings settings, AgentRegistry agentRegistry) {
        this.settings = settings;
        this.agentRegistry = agentRegistry;
    }

    public String syncInbox(String agentId, int limit) throws IOException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown agent: " + agentId);
        }
        List<EmailInboxMessage> messages = ImapInboxClient.fetchRecentMessages(settings, Math.max(1, limit), 500);
        Path summaryFile = profile.workspaceDir().resolve("inbox_summary.md");
        Files.createDirectories(profile.workspaceDir());
        StringBuilder out = new StringBuilder();
        out.append("# Inbox Summary\n\n");
        out.append("- Synced: ").append(LocalDateTime.now().format(STAMP)).append('\n');
        out.append("- Messages captured: ").append(messages.size()).append('\n');
        out.append("- Address: ").append(settings.address() == null || settings.address().isBlank() ? "(unspecified)" : settings.address().trim()).append("\n\n");
        if (messages.isEmpty()) {
            out.append("No inbox messages found.\n");
        } else {
            for (EmailInboxMessage message : messages) {
                out.append("## Message ").append(message.sequenceNumber()).append('\n');
                out.append("- From: ").append(blank(message.from(), "(unknown)")).append('\n');
                out.append("- Subject: ").append(blank(message.subject(), "(no subject)")).append('\n');
                out.append("- Date: ").append(blank(message.date(), "(unknown)")).append('\n');
                out.append("- Preview: ").append(blank(message.preview(), "(empty)")).append("\n\n");
            }
        }
        Files.writeString(summaryFile, out.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return "Synced " + messages.size() + " inbox messages to " + summaryFile;
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
