package helion;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class HttpBrowserTool implements BrowserTool {
    private final HttpClient httpClient;
    private final int defaultLimit;
    private final int fetchCharLimit;

    public HttpBrowserTool(int defaultLimit, int fetchCharLimit) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.defaultLimit = defaultLimit;
        this.fetchCharLimit = fetchCharLimit;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public BrowserSearchResult search(String query, int limit) throws IOException, InterruptedException {
        int actualLimit = Math.max(1, Math.min(10, limit <= 0 ? defaultLimit : limit));
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://duckduckgo.com/html/?q=" + encoded))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "helion/0.1")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Browser search failed: HTTP " + response.statusCode());
        }

        List<SearchResultItem> items = new ArrayList<>();
        extractDuckDuckGoResults(response.body(), actualLimit, items);

        String note = items.isEmpty() ? "No parsed results from DuckDuckGo HTML response." : "";
        return new BrowserSearchResult(query, items, note);
    }

    @Override
    public BrowserPage fetch(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "helion/0.1")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Browser fetch failed: HTTP " + response.statusCode() + " for " + url);
        }

        String html = response.body();
        String title = extractTitle(html);
        String content = TextUtils.limit(TextUtils.stripHtml(html), fetchCharLimit);
        return new BrowserPage(url, title, content, "Fetched from live web content.");
    }

    private String normalizeDuckDuckGoUrl(String href) {
        if (href.startsWith("//")) {
            href = "https:" + href;
        }
        if (!href.contains("uddg=")) {
            return href;
        }
        int start = href.indexOf("uddg=");
        int end = href.indexOf('&', start);
        String encoded = end >= 0 ? href.substring(start + 5, end) : href.substring(start + 5);
        return TextUtils.decodeUrl(encoded);
    }

    private void extractDuckDuckGoResults(String html, int limit, List<SearchResultItem> items) {
        String lower = html.toLowerCase();
        int cursor = 0;
        while (items.size() < limit) {
            int classPos = lower.indexOf("result__a", cursor);
            if (classPos < 0) {
                break;
            }
            int tagStart = lower.lastIndexOf("<a", classPos);
            int tagEnd = lower.indexOf('>', classPos);
            if (tagStart < 0 || tagEnd < 0) {
                break;
            }
            String anchorTag = html.substring(tagStart, tagEnd + 1);
            String href = extractAttribute(anchorTag, "href");
            int closeTag = lower.indexOf("</a>", tagEnd);
            if (closeTag < 0) {
                break;
            }
            String titleHtml = html.substring(tagEnd + 1, closeTag);
            int snippetEnd = lower.indexOf("</article>", closeTag);
            if (snippetEnd < 0) {
                int fallback = lower.indexOf("result__extras", closeTag);
                snippetEnd = fallback < 0 ? Math.min(html.length(), closeTag + 600) : fallback;
            }
            String snippetHtml = html.substring(closeTag + 4, Math.min(snippetEnd, html.length()));
            String title = TextUtils.stripHtml(titleHtml);
            String snippet = TextUtils.limit(TextUtils.stripHtml(snippetHtml), 240);
            String normalizedHref = normalizeDuckDuckGoUrl(href);
            if (!normalizedHref.isBlank() && !title.isBlank()) {
                items.add(new SearchResultItem(title, normalizedHref, snippet));
            }
            cursor = closeTag + 4;
        }
    }

    private String extractTitle(String html) {
        String lower = html.toLowerCase();
        int start = lower.indexOf("<title>");
        if (start < 0) {
            return "";
        }
        int end = lower.indexOf("</title>", start);
        if (end < 0) {
            return "";
        }
        return TextUtils.stripHtml(html.substring(start + 7, end));
    }

    private String extractAttribute(String tag, String attribute) {
        String lower = tag.toLowerCase();
        String marker = attribute.toLowerCase() + "=";
        int start = lower.indexOf(marker);
        if (start < 0) {
            return "";
        }
        int valueStart = start + marker.length();
        if (valueStart >= tag.length()) {
            return "";
        }
        char quote = tag.charAt(valueStart);
        if (quote == '"' || quote == '\'') {
            int end = tag.indexOf(quote, valueStart + 1);
            if (end > valueStart) {
                return tag.substring(valueStart + 1, end);
            }
            return "";
        }
        int end = valueStart;
        while (end < tag.length() && !Character.isWhitespace(tag.charAt(end)) && tag.charAt(end) != '>') {
            end++;
        }
        return tag.substring(valueStart, end);
    }
}
