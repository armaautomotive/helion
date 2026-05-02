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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HttpBrowserTool implements BrowserTool {
    private static final Pattern RESULT_PATTERN = Pattern.compile(
            "(?is)<a[^>]*class=\"[^\"]*result__a[^\"]*\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>(.*?)(?:</article>|<div class=\"result__extras\")");
    private static final Pattern TITLE_PATTERN = Pattern.compile("(?is)<title>(.*?)</title>");
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
        Matcher matcher = RESULT_PATTERN.matcher(response.body());
        while (matcher.find() && items.size() < actualLimit) {
            String href = normalizeDuckDuckGoUrl(matcher.group(1));
            String title = TextUtils.stripHtml(matcher.group(2));
            String snippet = TextUtils.limit(TextUtils.stripHtml(matcher.group(3)), 240);
            if (!href.isBlank() && !title.isBlank()) {
                items.add(new SearchResultItem(title, href, snippet));
            }
        }

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
        String title = "";
        Matcher titleMatcher = TITLE_PATTERN.matcher(html);
        if (titleMatcher.find()) {
            title = TextUtils.stripHtml(titleMatcher.group(1));
        }
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
}
