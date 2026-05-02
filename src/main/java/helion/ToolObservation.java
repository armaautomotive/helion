package helion;

public record ToolObservation(String type, String label, String content) {
    public String render() {
        StringBuilder out = new StringBuilder();
        out.append("TYPE: ").append(type).append('\n');
        out.append("LABEL: ").append(label).append('\n');
        out.append("CONTENT:\n").append(content.trim());
        return out.toString();
    }
}
