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
    private static final int SEARCH_RETRIES = 3;
    private final HttpClient httpClient;
    private final int defaultLimit;
    private final int fetchCharLimit;

    public HttpBrowserTool(int defaultLimit, int fetchCharLimit) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
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
        List<SearchResultItem> items = new ArrayList<>();
        String htmlUrl = "https://duckduckgo.com/html/?q=" + encoded;
        String liteUrl = "https://lite.duckduckgo.com/lite/?q=" + encoded;
        String note = "";

        HttpResponse<String> htmlResponse = sendSearchRequest(htmlUrl);
        extractDuckDuckGoResults(htmlResponse.body(), actualLimit, items);
        if (items.isEmpty()) {
            HttpResponse<String> liteResponse = sendSearchRequest(liteUrl);
            extractDuckDuckGoLiteResults(liteResponse.body(), actualLimit, items);
            note = items.isEmpty()
                    ? "No parsed results from DuckDuckGo HTML or Lite responses."
                    : "Results parsed from DuckDuckGo Lite fallback.";
        }
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

    private HttpResponse<String> sendSearchRequest(String url) throws IOException, InterruptedException {
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= SEARCH_RETRIES; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("User-Agent", "Mozilla/5.0 (compatible; Helion/0.1; +https://armaautomotive.com)")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException("Browser search failed: HTTP " + response.statusCode());
                }
                return response;
            } catch (IOException ex) {
                lastFailure = ex;
                if (attempt >= SEARCH_RETRIES) {
                    break;
                }
                Thread.sleep(searchBackoffMillis(attempt));
            }
        }
        throw lastFailure == null ? new IOException("Browser search failed.") : lastFailure;
    }

    private long searchBackoffMillis(int attempt) {
        return switch (attempt) {
            case 1 -> 1500L;
            case 2 -> 4000L;
            default -> 8000L;
        };
    }

    private void extractDuckDuckGoResults(String html, int limit, List<SearchResultItem> items) {
        extractClassBasedDuckDuckGoResults(html, limit, items);
        if (items.size() < limit) {
            extractGenericDuckDuckGoResults(html, limit, items);
        }
    }

    private void extractClassBasedDuckDuckGoResults(String html, int limit, List<SearchResultItem> items) {
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
            int closeTag = lower.indexOf("</a>", tagEnd);
            if (closeTag < 0) {
                break;
            }
            String anchorTag = html.substring(tagStart, tagEnd + 1);
            String titleHtml = html.substring(tagEnd + 1, closeTag);
            addSearchResult(items, anchorTag, titleHtml, html, closeTag, limit);
            cursor = closeTag + 4;
        }
    }

    private void extractGenericDuckDuckGoResults(String html, int limit, List<SearchResultItem> items) {
        String lower = html.toLowerCase();
        int cursor = 0;
        while (items.size() < limit) {
            int tagStart = lower.indexOf("<a", cursor);
            if (tagStart < 0) {
                break;
            }
            int tagEnd = lower.indexOf('>', tagStart);
            if (tagEnd < 0) {
                break;
            }
            int closeTag = lower.indexOf("</a>", tagEnd);
            if (closeTag < 0) {
                break;
            }
            String anchorTag = html.substring(tagStart, tagEnd + 1);
            String href = normalizeDuckDuckGoUrl(extractAttribute(anchorTag, "href"));
            String title = TextUtils.stripHtml(html.substring(tagEnd + 1, closeTag));
            if (looksLikeSearchResult(href, title, anchorTag, items)) {
                addSearchResult(items, anchorTag, html.substring(tagEnd + 1, closeTag), html, closeTag, limit);
            }
            cursor = closeTag + 4;
        }
    }

    private void extractDuckDuckGoLiteResults(String html, int limit, List<SearchResultItem> items) {
        String lower = html.toLowerCase();
        int cursor = 0;
        while (items.size() < limit) {
            int tagStart = lower.indexOf("<a", cursor);
            if (tagStart < 0) {
                break;
            }
            int tagEnd = lower.indexOf('>', tagStart);
            if (tagEnd < 0) {
                break;
            }
            int closeTag = lower.indexOf("</a>", tagEnd);
            if (closeTag < 0) {
                break;
            }
            String anchorTag = html.substring(tagStart, tagEnd + 1);
            String href = normalizeDuckDuckGoUrl(extractAttribute(anchorTag, "href"));
            String title = TextUtils.stripHtml(html.substring(tagEnd + 1, closeTag));
            if (looksLikeSearchResult(href, title, anchorTag, items)) {
                items.add(new SearchResultItem(title, href, ""));
            }
            cursor = closeTag + 4;
        }
    }

    private void addSearchResult(List<SearchResultItem> items, String anchorTag, String titleHtml, String html, int closeTag, int limit) {
        if (items.size() >= limit) {
            return;
        }
        String href = normalizeDuckDuckGoUrl(extractAttribute(anchorTag, "href"));
        String title = TextUtils.stripHtml(titleHtml);
        if (!looksLikeSearchResult(href, title, anchorTag, items)) {
            return;
        }
        String lower = html.toLowerCase();
        int snippetEnd = lower.indexOf("</article>", closeTag);
        if (snippetEnd < 0) {
            int fallback = lower.indexOf("result__extras", closeTag);
            snippetEnd = fallback < 0 ? Math.min(html.length(), closeTag + 600) : fallback;
        }
        String snippetHtml = html.substring(closeTag + 4, Math.min(snippetEnd, html.length()));
        String snippet = TextUtils.limit(TextUtils.stripHtml(snippetHtml), 240);
        items.add(new SearchResultItem(title, href, snippet));
    }

    private boolean looksLikeSearchResult(String href, String title, String anchorTag, List<SearchResultItem> items) {
        if (href.isBlank() || title.isBlank()) {
            return false;
        }
        String lowerHref = href.toLowerCase();
        String lowerTag = anchorTag.toLowerCase();
        if (!(lowerHref.startsWith("http://") || lowerHref.startsWith("https://"))) {
            return false;
        }
        if (lowerHref.contains("duckduckgo.com")) {
            return false;
        }
        if (lowerHref.startsWith("https://duckduckgo.com/y.js")) {
            return false;
        }
        if (lowerTag.contains("result__snippet") || lowerTag.contains("result__extras")) {
            return false;
        }
        for (SearchResultItem item : items) {
            if (item.url().equalsIgnoreCase(href)) {
                return false;
            }
        }
        return true;
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
