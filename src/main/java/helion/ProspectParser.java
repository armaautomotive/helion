package helion;

import java.util.ArrayList;
import java.util.List;

public final class ProspectParser {
    private static final String BLOCK_END = "<<<END_PROSPECT>>>";

    private ProspectParser() {
    }

    public static List<ProspectRecord> parse(String raw) {
        List<ProspectRecord> records = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return records;
        }
        String[] lines = raw.split("\n");
        List<String> block = new ArrayList<>();
        boolean inBlock = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if ("PROSPECT:".equalsIgnoreCase(trimmed)) {
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

    private static ProspectRecord parseBlock(List<String> lines) {
        String company = "";
        String website = "";
        String contactName = "";
        String contactRole = "";
        String contactEmail = "";
        String phone = "";
        String location = "";
        String industry = "";
        String fitScore = "";
        String status = "new";
        String priority = "medium";
        String owner = "unassigned";
        String discoveredAt = "";
        String lastUpdated = "";
        List<String> tags = new ArrayList<>();
        String whyFit = "";
        String evidence = "";
        String nextAction = "";
        List<String> sourceUrls = new ArrayList<>();
        boolean inSources = false;
        boolean inTags = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("- ") && inSources) {
                sourceUrls.add(trimmed.substring(2).trim());
                continue;
            }
            if (trimmed.startsWith("- ") && inTags) {
                tags.add(trimmed.substring(2).trim());
                continue;
            }
            inSources = false;
            inTags = false;
            if (startsWithLabel(trimmed, "Company:")) {
                company = valueAfter(trimmed, "Company:");
            } else if (startsWithLabel(trimmed, "Website:")) {
                website = valueAfter(trimmed, "Website:");
            } else if (startsWithLabel(trimmed, "Contact Name:")) {
                contactName = valueAfter(trimmed, "Contact Name:");
            } else if (startsWithLabel(trimmed, "Contact Role:")) {
                contactRole = valueAfter(trimmed, "Contact Role:");
            } else if (startsWithLabel(trimmed, "Contact Email:")) {
                contactEmail = valueAfter(trimmed, "Contact Email:");
            } else if (startsWithLabel(trimmed, "Phone:")) {
                phone = valueAfter(trimmed, "Phone:");
            } else if (startsWithLabel(trimmed, "Location:")) {
                location = valueAfter(trimmed, "Location:");
            } else if (startsWithLabel(trimmed, "Industry:")) {
                industry = valueAfter(trimmed, "Industry:");
            } else if (startsWithLabel(trimmed, "Fit Score:")) {
                fitScore = valueAfter(trimmed, "Fit Score:");
            } else if (startsWithLabel(trimmed, "Status:")) {
                status = valueAfter(trimmed, "Status:");
            } else if (startsWithLabel(trimmed, "Priority:")) {
                priority = valueAfter(trimmed, "Priority:");
            } else if (startsWithLabel(trimmed, "Owner:")) {
                owner = valueAfter(trimmed, "Owner:");
            } else if (startsWithLabel(trimmed, "Discovered At:")) {
                discoveredAt = valueAfter(trimmed, "Discovered At:");
            } else if (startsWithLabel(trimmed, "Last Updated:")) {
                lastUpdated = valueAfter(trimmed, "Last Updated:");
            } else if (startsWithLabel(trimmed, "Tags:")) {
                String firstTag = valueAfter(trimmed, "Tags:");
                if (!firstTag.isBlank()) {
                    tags.add(firstTag);
                }
                inTags = true;
            } else if (startsWithLabel(trimmed, "Why Fit:")) {
                whyFit = valueAfter(trimmed, "Why Fit:");
            } else if (startsWithLabel(trimmed, "Evidence:")) {
                evidence = valueAfter(trimmed, "Evidence:");
            } else if (startsWithLabel(trimmed, "Next Action:")) {
                nextAction = valueAfter(trimmed, "Next Action:");
            } else if (startsWithLabel(trimmed, "Source URLs:")) {
                String firstValue = valueAfter(trimmed, "Source URLs:");
                if (!firstValue.isBlank()) {
                    sourceUrls.add(firstValue);
                }
                inSources = true;
            }
        }

        return new ProspectRecord(company, website, contactName, contactRole, contactEmail, phone, location, industry, fitScore, status, priority, owner, discoveredAt, lastUpdated, tags, whyFit, evidence, sourceUrls, nextAction);
    }

    private static boolean startsWithLabel(String line, String label) {
        return line.regionMatches(true, 0, label, 0, label.length());
    }

    private static String valueAfter(String line, String label) {
        return line.substring(label.length()).trim();
    }
}
