package helion;

public record EmailSettings(
        boolean enabled,
        String provider,
        String displayName,
        String address,
        String imapHost,
        int imapPort,
        String imapUsername,
        String imapPassword,
        boolean imapSsl,
        String imapDraftsFolder,
        String smtpHost,
        int smtpPort,
        String smtpUsername,
        String smtpPassword,
        boolean smtpSsl) {

    public String describe() {
        if (!enabled) {
            return "Email disabled.";
        }
        return """
                Provider: %s
                Address: %s
                Display name: %s
                IMAP: %s:%d (%s)
                IMAP Drafts folder: %s
                SMTP: %s:%d (%s)
                """.formatted(
                valueOrDefault(provider, "unspecified"),
                valueOrDefault(address, "unspecified"),
                valueOrDefault(displayName, "unspecified"),
                valueOrDefault(imapHost, "unspecified"),
                imapPort,
                imapSsl ? "ssl" : "plain",
                valueOrDefault(imapDraftsFolder, "Drafts"),
                valueOrDefault(smtpHost, "unspecified"),
                smtpPort,
                smtpSsl ? "ssl" : "plain").trim();
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
