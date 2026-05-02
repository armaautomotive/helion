package helion;

public record BrowserPage(String url, String title, String content, String note) {
    public String render() {
        StringBuilder out = new StringBuilder();
        out.append("URL: ").append(url).append('\n');
        if (title != null && !title.isBlank()) {
            out.append("Title: ").append(title).append('\n');
        }
        if (note != null && !note.isBlank()) {
            out.append("Note: ").append(note).append('\n');
        }
        out.append("Content:\n").append(content);
        return out.toString().trim();
    }
}
