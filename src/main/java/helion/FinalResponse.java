package helion;

import java.util.List;

public record FinalResponse(
        String status,
        String title,
        String summary,
        String details,
        List<String> nextSteps,
        List<String> sources) {

    public String render() {
        StringBuilder out = new StringBuilder();
        if (!title.isBlank()) {
            out.append(title.trim()).append('\n').append('\n');
        }
        if (!summary.isBlank()) {
            out.append("Summary\n");
            out.append(summary.trim()).append('\n').append('\n');
        }
        if (!details.isBlank()) {
            out.append("Details\n");
            out.append(details.trim()).append('\n').append('\n');
        }
        if (!nextSteps.isEmpty()) {
            out.append("Next Steps\n");
            for (int i = 0; i < nextSteps.size(); i++) {
                out.append(i + 1).append(". ").append(nextSteps.get(i)).append('\n');
            }
            out.append('\n');
        }
        if (!sources.isEmpty()) {
            out.append("Sources\n");
            for (String source : sources) {
                out.append("- ").append(source).append('\n');
            }
            out.append('\n');
        }
        if (!status.isBlank()) {
            out.append("Status: ").append(status.trim());
        }
        return out.toString().trim();
    }
}
