package helion;

import java.util.List;

public record BrowserSearchResult(String query, List<SearchResultItem> items, String note) {
    public String render() {
        StringBuilder out = new StringBuilder();
        out.append("Query: ").append(query).append('\n');
        if (note != null && !note.isBlank()) {
            out.append("Note: ").append(note).append('\n');
        }
        if (items.isEmpty()) {
            out.append("No results.\n");
            return out.toString().trim();
        }
        for (int i = 0; i < items.size(); i++) {
            SearchResultItem item = items.get(i);
            out.append(i + 1).append(". ").append(item.title()).append('\n');
            out.append("URL: ").append(item.url()).append('\n');
            if (!item.snippet().isBlank()) {
                out.append("Snippet: ").append(item.snippet()).append('\n');
            }
        }
        return out.toString().trim();
    }
}
