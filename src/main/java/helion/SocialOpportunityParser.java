package helion;

import java.util.ArrayList;
import java.util.List;

public final class SocialOpportunityParser {
    private static final String BLOCK_END = "<<<END_OPPORTUNITY>>>";

    private SocialOpportunityParser() {
    }

    public static List<SocialOpportunityRecord> parse(String raw) {
        List<SocialOpportunityRecord> records = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return records;
        }
        String[] lines = raw.split("\n");
        List<String> block = new ArrayList<>();
        boolean inBlock = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if ("OPPORTUNITY:".equalsIgnoreCase(trimmed)) {
                if (!block.isEmpty()) {
                    records.add(parseBlock(block));
                    block.clear();
                }
                inBlock = true;
                continue;
            }
            if (BLOCK_END.equalsIgnoreCase(trimmed)) {
                if (!block.isEmpty()) {
                    records.add(parseBlock(block));
                    block.clear();
                }
                inBlock = false;
                continue;
            }
            if (inBlock) {
                block.add(line);
            }
        }
        if (!block.isEmpty()) {
            records.add(parseBlock(block));
        }
        return records;
    }

    private static SocialOpportunityRecord parseBlock(List<String> lines) {
        String title = "";
        String url = "";
        String site = "";
        String community = "";
        String author = "";
        String posted = "";
        String relevance = "";
        String buyerSignal = "";
        String productFit = "";
        String recommendedAngle = "";
        String evidence = "";
        String status = "new";
        List<String> tags = new ArrayList<>();
        List<String> sourceUrls = new ArrayList<>();
        boolean inTags = false;
        boolean inSources = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("- ") && inTags) {
                tags.add(trimmed.substring(2).trim());
                continue;
            }
            if (trimmed.startsWith("- ") && inSources) {
                sourceUrls.add(trimmed.substring(2).trim());
                continue;
            }
            inTags = false;
            inSources = false;
            if (startsWithLabel(trimmed, "Title:")) {
                title = valueAfter(trimmed, "Title:");
            } else if (startsWithLabel(trimmed, "URL:")) {
                url = valueAfter(trimmed, "URL:");
            } else if (startsWithLabel(trimmed, "Site:")) {
                site = valueAfter(trimmed, "Site:");
            } else if (startsWithLabel(trimmed, "Community:")) {
                community = valueAfter(trimmed, "Community:");
            } else if (startsWithLabel(trimmed, "Author:")) {
                author = valueAfter(trimmed, "Author:");
            } else if (startsWithLabel(trimmed, "Posted:")) {
                posted = valueAfter(trimmed, "Posted:");
            } else if (startsWithLabel(trimmed, "Relevance:")) {
                relevance = valueAfter(trimmed, "Relevance:");
            } else if (startsWithLabel(trimmed, "Buyer Signal:")) {
                buyerSignal = valueAfter(trimmed, "Buyer Signal:");
            } else if (startsWithLabel(trimmed, "Product Fit:")) {
                productFit = valueAfter(trimmed, "Product Fit:");
            } else if (startsWithLabel(trimmed, "Recommended Angle:")) {
                recommendedAngle = valueAfter(trimmed, "Recommended Angle:");
            } else if (startsWithLabel(trimmed, "Evidence:")) {
                evidence = valueAfter(trimmed, "Evidence:");
            } else if (startsWithLabel(trimmed, "Status:")) {
                status = valueAfter(trimmed, "Status:");
            } else if (startsWithLabel(trimmed, "Tags:")) {
                String first = valueAfter(trimmed, "Tags:");
                if (!first.isBlank()) {
                    tags.add(first);
                }
                inTags = true;
            } else if (startsWithLabel(trimmed, "Source URLs:")) {
                String first = valueAfter(trimmed, "Source URLs:");
                if (!first.isBlank()) {
                    sourceUrls.add(first);
                }
                inSources = true;
            }
        }
        if (sourceUrls.isEmpty() && !url.isBlank()) {
            sourceUrls.add(url);
        }
        return new SocialOpportunityRecord(title, url, site, community, author, posted, relevance, buyerSignal, productFit, recommendedAngle, evidence, status, tags, sourceUrls);
    }

    private static boolean startsWithLabel(String line, String label) {
        return line.regionMatches(true, 0, label, 0, label.length());
    }

    private static String valueAfter(String line, String label) {
        return line.substring(label.length()).trim();
    }
}
