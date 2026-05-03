package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public final class EmailDraftStore {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final java.time.format.DateTimeFormatter RFC_2822 =
            new DateTimeFormatterBuilder().appendPattern("EEE, dd MMM yyyy HH:mm:ss Z").toFormatter(Locale.US);

    private final EmailSettings settings;
    private final AgentRegistry agentRegistry;
    private final HelionConfig config;

    public EmailDraftStore(EmailSettings settings, AgentRegistry agentRegistry, HelionConfig config) {
        this.settings = settings;
        this.agentRegistry = agentRegistry;
        this.config = config;
    }

    public boolean isEnabled() {
        return settings.enabled();
    }

    public String describe() {
        return settings.describe();
    }

    public String saveDraft(String agentId, String to, String subject, String body, String notes) throws IOException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown agent: " + agentId);
        }
        Path draftsFile = AgentOutputResolver.resolvePrimaryOutputFile(profile, config, "workspace/reply_drafts.md");
        Files.createDirectories(profile.workspaceDir());
        if (draftsFile.getParent() != null) {
            Files.createDirectories(draftsFile.getParent());
        }
        if (!Files.exists(draftsFile)) {
            Files.writeString(draftsFile, "# Reply Drafts\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        }
        String entry = """

                ## Draft %s
                - To: %s
                - Subject: %s
                - From: %s
                - Status: draft-only
                - Notes: %s

                ### Body
                %s
                """.formatted(
                LocalDateTime.now().format(STAMP),
                blankFallback(to, "(unspecified)"),
                blankFallback(subject, "(no subject)"),
                blankFallback(settings.address(), "(email not configured)"),
                blankFallback(notes, "(none)"),
                blankFallback(body, "(empty draft)"));
        Files.writeString(draftsFile, entry, StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        boolean uploaded = false;
        String uploadNote = "";
        if (settings.enabled()) {
            try {
                ImapInboxClient.appendDraft(settings, buildMimeDraft(to, subject, body));
                uploaded = true;
            } catch (IOException ex) {
                uploadNote = " Server draft upload failed: " + ex.getMessage();
            }
        }
        return "Saved draft email to " + draftsFile + (uploaded ? " and uploaded to IMAP Drafts." : "." + uploadNote);
    }

    private String buildMimeDraft(String to, String subject, String body) {
        String fromAddress = blankFallback(settings.address(), "support@example.com");
        String fromName = blankFallback(settings.displayName(), fromAddress);
        StringBuilder out = new StringBuilder();
        out.append("Date: ").append(RFC_2822.format(ZonedDateTime.now())).append("\r\n");
        out.append("From: ").append(formatMailbox(fromName, fromAddress)).append("\r\n");
        if (to != null && !to.isBlank()) {
            out.append("To: ").append(to.trim()).append("\r\n");
        }
        out.append("Subject: ").append(blankFallback(subject, "(no subject)")).append("\r\n");
        out.append("MIME-Version: 1.0\r\n");
        out.append("Content-Type: text/plain; charset=UTF-8\r\n");
        out.append("Content-Transfer-Encoding: 8bit\r\n");
        out.append("X-Unsent: 1\r\n");
        out.append("\r\n");
        out.append(blankFallback(body, "(empty draft)")).append("\r\n");
        return out.toString();
    }

    private String formatMailbox(String name, String address) {
        if (name == null || name.isBlank() || name.equals(address)) {
            return address;
        }
        return "\"" + name.replace("\"", "'") + "\" <" + address + ">";
    }

    private static String blankFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
