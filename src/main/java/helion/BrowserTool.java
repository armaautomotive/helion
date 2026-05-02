package helion;

import java.io.IOException;

public interface BrowserTool {
    boolean isEnabled();

    BrowserSearchResult search(String query, int limit) throws IOException, InterruptedException;

    BrowserPage fetch(String url) throws IOException, InterruptedException;
}
