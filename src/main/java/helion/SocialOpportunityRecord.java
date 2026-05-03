package helion;

import java.util.ArrayList;
import java.util.List;

public record SocialOpportunityRecord(
        String title,
        String url,
        String site,
        String community,
        String author,
        String posted,
        String relevance,
        String buyerSignal,
        String productFit,
        String recommendedAngle,
        String evidence,
        String status,
        List<String> tags,
        List<String> sourceUrls) {

    public String toMarkdown() {
        StringBuilder out = new StringBuilder();
        out.append("## ").append(valueOrDefault(title, "Untitled opportunity")).append('\n');
        out.append("- URL: ").append(valueOrDefault(url, "")).append('\n');
        out.append("- Site: ").append(valueOrDefault(site, "")).append('\n');
        out.append("- Community: ").append(valueOrDefault(community, "")).append('\n');
        out.append("- Author: ").append(valueOrDefault(author, "")).append('\n');
        out.append("- Posted: ").append(valueOrDefault(posted, "")).append('\n');
        out.append("- Relevance: ").append(valueOrDefault(relevance, "")).append('\n');
        out.append("- Buyer signal: ").append(valueOrDefault(buyerSignal, "")).append('\n');
        out.append("- Product fit: ").append(valueOrDefault(productFit, "")).append('\n');
        out.append("- Recommended angle: ").append(valueOrDefault(recommendedAngle, "")).append('\n');
        out.append("- Evidence: ").append(valueOrDefault(evidence, "")).append('\n');
        out.append("- Status: ").append(valueOrDefault(status, "new")).append('\n');
        if (tags != null && !tags.isEmpty()) {
            out.append("- Tags: ").append(String.join(", ", tags)).append('\n');
        }
        if (sourceUrls != null && !sourceUrls.isEmpty()) {
            out.append("- Source URLs: ").append(String.join(", ", sourceUrls)).append('\n');
        }
        return out.toString().trim();
    }

    public String toCsvRow() {
        return String.join(",",
                csv(title),
                csv(url),
                csv(site),
                csv(community),
                csv(author),
                csv(posted),
                csv(relevance),
                csv(buyerSignal),
                csv(productFit),
                csv(recommendedAngle),
                csv(evidence),
                csv(status),
                csv(tags == null ? "" : String.join("|", tags)),
                csv(sourceUrls == null ? "" : String.join("|", sourceUrls)));
    }

    public List<String> dedupeKeys() {
        List<String> keys = new ArrayList<>();
        String normalizedUrl = normalize(url);
        String normalizedTitle = normalize(title);
        if (!normalizedUrl.isBlank()) {
            keys.add("url:" + normalizedUrl);
        }
        if (!normalizedTitle.isBlank()) {
            keys.add("title:" + normalizedTitle);
        }
        return keys;
    }

    private static String csv(String value) {
        String text = valueOrDefault(value, "");
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String lower = value.trim().toLowerCase();
        StringBuilder out = new StringBuilder(lower.length());
        boolean lastWasSeparator = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                out.append(c);
                lastWasSeparator = false;
            } else if (!lastWasSeparator) {
                out.append(' ');
                lastWasSeparator = true;
            }
        }
        return out.toString().trim();
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
