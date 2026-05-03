package helion;

import java.util.ArrayList;
import java.util.List;

public record ProspectRecord(
        String company,
        String website,
        String contactName,
        String contactRole,
        String contactEmail,
        String phone,
        String location,
        String industry,
        String fitScore,
        String status,
        String priority,
        String owner,
        String discoveredAt,
        String lastUpdated,
        List<String> tags,
        String whyFit,
        String evidence,
        List<String> sourceUrls,
        String nextAction) {

    public String toMarkdown() {
        StringBuilder out = new StringBuilder();
        out.append("## ").append(valueOrDefault(company, "Unknown company")).append('\n');
        out.append("- Website: ").append(valueOrDefault(website, "")).append('\n');
        out.append("- Contact name: ").append(valueOrDefault(contactName, "")).append('\n');
        out.append("- Contact role: ").append(valueOrDefault(contactRole, "")).append('\n');
        out.append("- Contact email: ").append(valueOrDefault(contactEmail, "")).append('\n');
        out.append("- Phone: ").append(valueOrDefault(phone, "")).append('\n');
        out.append("- Location: ").append(valueOrDefault(location, "")).append('\n');
        out.append("- Industry: ").append(valueOrDefault(industry, "")).append('\n');
        out.append("- Fit score: ").append(valueOrDefault(fitScore, "")).append('\n');
        out.append("- Status: ").append(valueOrDefault(status, "new")).append('\n');
        out.append("- Priority: ").append(valueOrDefault(priority, "medium")).append('\n');
        out.append("- Owner: ").append(valueOrDefault(owner, "unassigned")).append('\n');
        out.append("- Discovered at: ").append(valueOrDefault(discoveredAt, "")).append('\n');
        out.append("- Last updated: ").append(valueOrDefault(lastUpdated, "")).append('\n');
        if (tags != null && !tags.isEmpty()) {
            out.append("- Tags: ").append(String.join(", ", tags)).append('\n');
        }
        out.append("- Why they might fit: ").append(valueOrDefault(whyFit, "")).append('\n');
        out.append("- Tube fabrication evidence: ").append(valueOrDefault(evidence, "")).append('\n');
        out.append("- Likely buyer: ").append(valueOrDefault(contactRole, "")).append('\n');
        out.append("- Next action: ").append(valueOrDefault(nextAction, "")).append('\n');
        if (sourceUrls != null && !sourceUrls.isEmpty()) {
            out.append("- Sources: ").append(String.join(", ", sourceUrls)).append('\n');
        }
        return out.toString().trim();
    }

    public String toCsvRow() {
        return String.join(",",
                csv(company),
                csv(website),
                csv(contactName),
                csv(contactRole),
                csv(contactEmail),
                csv(phone),
                csv(location),
                csv(industry),
                csv(fitScore),
                csv(status),
                csv(priority),
                csv(owner),
                csv(discoveredAt),
                csv(lastUpdated),
                csv(tags == null ? "" : String.join(" | ", tags)),
                csv(whyFit),
                csv(evidence),
                csv(sourceUrls == null ? "" : String.join(" | ", sourceUrls)),
                csv(nextAction));
    }

    public List<String> dedupeKeys() {
        List<String> keys = new ArrayList<>();
        String websiteKey = normalizedWebsite();
        String emailKey = normalizedContactEmail();
        String companyKey = normalizedCompany();
        String contactNameKey = normalize(contactName);
        String contactRoleKey = normalize(contactRole);

        if (!websiteKey.isBlank()) {
            keys.add("website:" + websiteKey);
        }
        if (!emailKey.isBlank()) {
            keys.add("email:" + emailKey);
        }
        if (!companyKey.isBlank() && !contactNameKey.isBlank()) {
            keys.add("company-contact:" + companyKey + "|" + contactNameKey);
        }
        if (!companyKey.isBlank() && !contactRoleKey.isBlank()) {
            keys.add("company-role:" + companyKey + "|" + contactRoleKey);
        }
        if (!companyKey.isBlank()) {
            keys.add("company:" + companyKey);
        }
        return keys;
    }

    private static String csv(String value) {
        String text = valueOrDefault(value, "");
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private String normalizedWebsite() {
        String value = normalize(website);
        if (value.startsWith("https://")) {
            value = value.substring("https://".length());
        } else if (value.startsWith("http://")) {
            value = value.substring("http://".length());
        }
        if (value.startsWith("www.")) {
            value = value.substring("www.".length());
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String normalizedContactEmail() {
        return normalize(contactEmail);
    }

    private String normalizedCompany() {
        return normalize(company);
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
