package helion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class EmailDraftStore {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EmailSettings settings;
    private final AgentRegistry agentRegistry;

    public EmailDraftStore(EmailSettings settings, AgentRegistry agentRegistry) {
        this.settings = settings;
        this.agentRegistry = agentRegistry;
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
        Path draftsFile = profile.workspaceDir().resolve("reply_drafts.md");
        Files.createDirectories(profile.workspaceDir());
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
        return "Saved draft email to " + draftsFile;
    }

    private static String blankFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
