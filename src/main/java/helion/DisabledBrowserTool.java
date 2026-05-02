package helion;

import java.io.IOException;
import java.util.List;

public final class DisabledBrowserTool implements BrowserTool {
    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public BrowserSearchResult search(String query, int limit) throws IOException {
        return new BrowserSearchResult(query, List.of(), "Browser is disabled. Set HELION_ENABLE_BROWSER=true.");
    }

    @Override
    public BrowserPage fetch(String url) throws IOException {
        return new BrowserPage(url, "", "", "Browser is disabled. Set HELION_ENABLE_BROWSER=true.");
    }
}
