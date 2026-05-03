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
        return renderStyled(true);
    }

    public String renderPlain() {
        return renderStyled(false);
    }

    private String renderStyled(boolean ansi) {
        StringBuilder out = new StringBuilder();
        if (!title.isBlank()) {
            out.append(styleTitle(title.trim(), ansi)).append('\n').append('\n');
        }
        if (!summary.isBlank()) {
            out.append(styleSectionLabel("Summary", ansi)).append('\n');
            out.append(summary.trim()).append('\n').append('\n');
        }
        if (!details.isBlank()) {
            out.append(styleSectionLabel("Details", ansi)).append('\n');
            out.append(details.trim()).append('\n').append('\n');
        }
        if (!nextSteps.isEmpty()) {
            out.append(styleSectionLabel("Next Steps", ansi)).append('\n');
            for (int i = 0; i < nextSteps.size(); i++) {
                out.append(i + 1).append(". ").append(nextSteps.get(i)).append('\n');
            }
            out.append('\n');
        }
        if (!sources.isEmpty()) {
            out.append(styleSectionLabel("Sources", ansi)).append('\n');
            for (String source : sources) {
                out.append("- ").append(source).append('\n');
            }
            out.append('\n');
        }
        if (!status.isBlank()) {
            out.append(styleStatusPrefix(ansi)).append(status.trim());
        }
        return out.toString().trim();
    }

    private static String styleTitle(String value, boolean ansi) {
        return ansi ? Ansi.bold(value) : value;
    }

    private static String styleSectionLabel(String value, boolean ansi) {
        return ansi ? Ansi.blue(value) : value;
    }

    private static String styleStatusPrefix(boolean ansi) {
        return ansi ? Ansi.green("Status: ") : "Status: ";
    }
}
