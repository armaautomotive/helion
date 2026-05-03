package helion;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class TextUtils {
    private TextUtils() {
    }

    public static String escapeJson(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        appendUnicodeEscape(out, c);
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    public static String unescapeJson(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!escaping) {
                if (c == '\\') {
                    escaping = true;
                } else {
                    out.append(c);
                }
                continue;
            }
            switch (c) {
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'u' -> {
                    if (i + 4 < value.length()) {
                        String hex = value.substring(i + 1, i + 5);
                        try {
                            out.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } catch (NumberFormatException ignored) {
                            out.append('u');
                        }
                    } else {
                        out.append('u');
                    }
                }
                default -> out.append(c);
            }
            escaping = false;
        }
        if (escaping) {
            out.append('\\');
        }
        return out.toString();
    }

    public static String stripHtml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String withoutScripts = removeTagBlock(value, "script");
        String withoutStyles = removeTagBlock(withoutScripts, "style");
        String withoutTags = stripTags(withoutStyles);
        String decoded = withoutTags
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        return collapseWhitespace(decoded).trim();
    }

    public static String limit(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    public static String decodeUrl(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    public static String readJsonStringValue(String text, int start) {
        StringBuilder out = new StringBuilder();
        boolean escaping = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaping) {
                switch (c) {
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'u' -> {
                        if (i + 4 < text.length()) {
                            String hex = text.substring(i + 1, i + 5);
                            try {
                                out.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException ignored) {
                                out.append('u');
                            }
                        } else {
                            out.append('u');
                        }
                    }
                    default -> out.append(c);
                }
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '"') {
                break;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static void appendUnicodeEscape(StringBuilder out, char c) {
        out.append("\\u");
        String hex = Integer.toHexString(c);
        for (int i = hex.length(); i < 4; i++) {
            out.append('0');
        }
        out.append(hex);
    }

    private static String removeTagBlock(String value, String tagName) {
        String lower = value.toLowerCase();
        String open = "<" + tagName;
        String close = "</" + tagName + ">";
        StringBuilder out = new StringBuilder(value.length());
        int cursor = 0;

        while (cursor < value.length()) {
            int start = lower.indexOf(open, cursor);
            if (start < 0) {
                out.append(value, cursor, value.length());
                break;
            }
            out.append(value, cursor, start);
            int end = lower.indexOf(close, start);
            if (end < 0) {
                break;
            }
            cursor = end + close.length();
            out.append(' ');
        }
        return out.toString();
    }

    private static String stripTags(String value) {
        StringBuilder out = new StringBuilder(value.length());
        boolean inTag = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '<') {
                inTag = true;
                out.append(' ');
                continue;
            }
            if (c == '>') {
                inTag = false;
                continue;
            }
            if (!inTag) {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String collapseWhitespace(String value) {
        StringBuilder out = new StringBuilder(value.length());
        boolean lastWasWhitespace = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!lastWasWhitespace) {
                    out.append(' ');
                    lastWasWhitespace = true;
                }
            } else {
                out.append(c);
                lastWasWhitespace = false;
            }
        }
        return out.toString();
    }
}
