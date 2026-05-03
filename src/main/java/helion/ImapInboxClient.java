package helion;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLSocketFactory;

public final class ImapInboxClient {
    private ImapInboxClient() {
    }

    public static List<EmailInboxMessage> fetchRecentMessages(EmailSettings settings, int limit, int previewChars) throws IOException {
        if (!settings.enabled()) {
            throw new IOException("Email is disabled.");
        }
        if (blank(settings.imapHost()) || blank(settings.imapUsername()) || blank(settings.imapPassword())) {
            throw new IOException("IMAP settings are incomplete.");
        }

        try (Socket socket = openSocket(settings);
             BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
             BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream())) {
            ImapSession session = new ImapSession(input, output);
            session.readGreeting();
            session.command("LOGIN " + quoted(settings.imapUsername()) + " " + quoted(settings.imapPassword()));
            session.command("EXAMINE INBOX");
            String search = session.command("SEARCH ALL");
            List<Integer> ids = parseSearchIds(search);
            if (ids.isEmpty()) {
                session.command("LOGOUT");
                return List.of();
            }
            int fromIndex = Math.max(0, ids.size() - Math.max(1, limit));
            List<EmailInboxMessage> messages = new ArrayList<>();
            for (int i = ids.size() - 1; i >= fromIndex; i--) {
                int seq = ids.get(i);
                String response = session.command("FETCH " + seq + " (BODY.PEEK[HEADER.FIELDS (SUBJECT FROM DATE)] BODY.PEEK[TEXT]<0." + Math.max(200, previewChars) + ">)");
                messages.add(parseMessage(seq, response, previewChars));
            }
            session.command("LOGOUT");
            return messages;
        }
    }

    public static void appendDraft(EmailSettings settings, String mimeMessage) throws IOException {
        if (!settings.enabled()) {
            throw new IOException("Email is disabled.");
        }
        if (blank(settings.imapHost()) || blank(settings.imapUsername()) || blank(settings.imapPassword())) {
            throw new IOException("IMAP settings are incomplete.");
        }
        String folder = blank(settings.imapDraftsFolder()) ? "Drafts" : settings.imapDraftsFolder().trim();
        String payload = mimeMessage == null ? "" : mimeMessage.replace("\r\n", "\n").replace("\n", "\r\n");

        try (Socket socket = openSocket(settings);
             BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
             BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream())) {
            ImapSession session = new ImapSession(input, output);
            session.readGreeting();
            session.command("LOGIN " + quoted(settings.imapUsername()) + " " + quoted(settings.imapPassword()));
            session.append(folder, payload);
            session.command("LOGOUT");
        }
    }

    private static Socket openSocket(EmailSettings settings) throws IOException {
        if (settings.imapSsl()) {
            return SSLSocketFactory.getDefault().createSocket(settings.imapHost(), settings.imapPort());
        }
        return new Socket(settings.imapHost(), settings.imapPort());
    }

    private static List<Integer> parseSearchIds(String response) {
        List<Integer> ids = new ArrayList<>();
        for (String line : response.split("\r?\n")) {
            String trimmed = line.trim();
            if (!trimmed.toUpperCase(Locale.ROOT).startsWith("* SEARCH")) {
                continue;
            }
            String[] parts = trimmed.substring(8).trim().split("\\s+");
            for (String part : parts) {
                try {
                    ids.add(Integer.parseInt(part.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return ids;
    }

    private static EmailInboxMessage parseMessage(int sequenceNumber, String response, int previewChars) {
        String from = "";
        String subject = "";
        String date = "";
        StringBuilder preview = new StringBuilder();
        boolean inHeaders = true;
        for (String line : response.split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("* ") || trimmed.startsWith("A")) {
                continue;
            }
            if (trimmed.isEmpty()) {
                inHeaders = false;
                continue;
            }
            if (inHeaders) {
                if (trimmed.regionMatches(true, 0, "From:", 0, 5)) {
                    from = trimmed.substring(5).trim();
                } else if (trimmed.regionMatches(true, 0, "Subject:", 0, 8)) {
                    subject = trimmed.substring(8).trim();
                } else if (trimmed.regionMatches(true, 0, "Date:", 0, 5)) {
                    date = trimmed.substring(5).trim();
                }
            } else if (preview.length() < previewChars) {
                if (!preview.isEmpty()) {
                    preview.append(' ');
                }
                preview.append(trimmed);
            }
        }
        String previewText = TextUtils.limit(preview.toString().replace('\u0000', ' ').trim(), previewChars);
        return new EmailInboxMessage(sequenceNumber, from, subject, date, previewText);
    }

    private static String quoted(String value) {
        return "\"" + (value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"";
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static final class ImapSession {
        private final BufferedInputStream input;
        private final BufferedOutputStream output;
        private int counter = 1;

        private ImapSession(BufferedInputStream input, BufferedOutputStream output) {
            this.input = input;
            this.output = output;
        }

        void readGreeting() throws IOException {
            String line = readLine();
            if (line == null || (!line.startsWith("* OK") && !line.startsWith("* PREAUTH"))) {
                throw new IOException("Unexpected IMAP greeting.");
            }
        }

        String command(String command) throws IOException {
            String tag = String.format("A%04d", counter++);
            writeLine(tag + " " + command);
            StringBuilder response = new StringBuilder();
            while (true) {
                String line = readLine();
                if (line == null) {
                    throw new IOException("IMAP connection closed.");
                }
                response.append(line).append("\n");
                int literalLength = literalLength(line);
                if (literalLength > 0) {
                    String literal = readLiteral(literalLength);
                    response.append(literal).append("\n");
                }
                if (line.startsWith(tag + " ")) {
                    if (!line.toUpperCase(Locale.ROOT).contains(" OK")) {
                        throw new IOException("IMAP command failed: " + line);
                    }
                    return response.toString().trim();
                }
            }
        }

        void append(String folder, String payload) throws IOException {
            String tag = String.format("A%04d", counter++);
            writeLine(tag + " APPEND " + quoted(folder) + " {" + payload.getBytes(StandardCharsets.UTF_8).length + "}");
            String continuation = readLine();
            if (continuation == null || !continuation.startsWith("+")) {
                throw new IOException("IMAP APPEND was not accepted.");
            }
            output.write(payload.getBytes(StandardCharsets.UTF_8));
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
            output.flush();

            while (true) {
                String line = readLine();
                if (line == null) {
                    throw new IOException("IMAP connection closed during APPEND.");
                }
                if (line.startsWith(tag + " ")) {
                    if (!line.toUpperCase(Locale.ROOT).contains(" OK")) {
                        throw new IOException("IMAP APPEND failed: " + line);
                    }
                    return;
                }
            }
        }

        private void writeLine(String line) throws IOException {
            output.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.flush();
        }

        private String readLine() throws IOException {
            StringBuilder line = new StringBuilder();
            int previous = -1;
            while (true) {
                int value = input.read();
                if (value < 0) {
                    return line.isEmpty() ? null : line.toString();
                }
                if (previous == '\r' && value == '\n') {
                    line.setLength(Math.max(0, line.length() - 1));
                    return line.toString();
                }
                line.append((char) value);
                previous = value;
            }
        }

        private String readLiteral(int length) throws IOException {
            byte[] buffer = input.readNBytes(length);
            if (buffer.length != length) {
                throw new IOException("Incomplete IMAP literal.");
            }
            return new String(buffer, StandardCharsets.UTF_8);
        }

        private int literalLength(String line) {
            String trimmed = line == null ? "" : line.trim();
            if (!trimmed.endsWith("}")) {
                return 0;
            }
            int start = trimmed.lastIndexOf('{');
            if (start < 0) {
                return 0;
            }
            try {
                return Integer.parseInt(trimmed.substring(start + 1, trimmed.length() - 1));
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
    }
}
