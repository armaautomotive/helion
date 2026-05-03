package helion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public final class DemoBusinessProvider implements LlmProvider {
    @Override
    public String name() {
        return "demo";
    }

    @Override
    public String modelName() {
        return "demo";
    }

    @Override
    public LlmResult chatResult(List<LlmMessage> messages) throws IOException {
        String userPrompt = messages.isEmpty() ? "" : messages.get(messages.size() - 1).content();
        String request = extractBusinessRequest(userPrompt);
        String title = summarize(request);
        List<String> risks = inferRisks(request);
        StringJoiner assumptions = inferAssumptions(request);

        StringBuilder out = new StringBuilder();
        out.append("Helion Business Brief\n");
        out.append('\n');
        out.append("Focus\n");
        out.append(title).append('\n');
        out.append('\n');
        out.append("Assessment\n");
        out.append("The request suggests a need for sharper positioning, a faster feedback loop, and a clearer revenue path.\n");
        out.append('\n');
        out.append("Assumptions\n");
        out.append(assumptions).append('\n');
        out.append('\n');
        out.append("Recommended Actions\n");
        out.append("1. Define the customer segment, budget owner, and buying trigger in one sentence.\n");
        out.append("2. Offer a narrowly scoped first service or product that can be sold in under 30 days.\n");
        out.append("3. Build a weekly operating cadence around pipeline, delivery, and customer feedback.\n");
        out.append('\n');
        out.append("Near-Term Plan\n");
        out.append("- Week 1: validate the problem with 5 target customers.\n");
        out.append("- Week 2: package the offer, pricing, and proof points.\n");
        out.append("- Week 3: run direct outreach and track objections.\n");
        out.append("- Week 4: refine messaging based on close rate and response quality.\n");
        out.append('\n');
        out.append("Risks\n");
        for (String risk : risks) {
            out.append("- ").append(risk).append('\n');
        }
        out.append('\n');
        out.append("Input\n");
        out.append(request).append('\n');
        String response = out.toString();
        int promptTokens = Math.max(1, (int) Math.ceil(userPrompt.length() / 4.0));
        int completionTokens = Math.max(1, (int) Math.ceil(response.length() / 4.0));
        return new LlmResult(response, new UsageMetrics(name(), modelName(), promptTokens, completionTokens, promptTokens + completionTokens, false));
    }

    private String extractBusinessRequest(String userPrompt) {
        int index = userPrompt.indexOf("Business request:");
        if (index >= 0) {
            return userPrompt.substring(index + "Business request:".length()).trim();
        }
        return userPrompt.trim();
    }

    private String summarize(String request) {
        if (request.isBlank()) {
            return "Clarify the business objective, target customer, and timeline.";
        }
        String normalized = request.replace('\n', ' ').trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 117) + "...";
    }

    private List<String> inferRisks(String request) {
        String normalized = request.toLowerCase();
        List<String> risks = new ArrayList<>();
        risks.add("Weak differentiation can turn sales into price competition.");
        if (!normalized.contains("customer")) {
            risks.add("The target customer is not explicit, which will weaken positioning.");
        }
        if (!normalized.contains("price") && !normalized.contains("revenue")) {
            risks.add("The revenue model is unclear and may delay execution decisions.");
        }
        if (!normalized.contains("timeline") && !normalized.contains("month")) {
            risks.add("No explicit timeline makes it hard to evaluate progress.");
        }
        return risks;
    }

    private StringJoiner inferAssumptions(String request) {
        String normalized = request.toLowerCase();
        StringJoiner assumptions = new StringJoiner("\n");
        assumptions.add("- The business needs an offer that is easier to explain and easier to buy.");
        assumptions.add(normalized.contains("enterprise")
                ? "- The sales cycle is likely longer and proof points will matter."
                : "- The initial go-to-market should bias toward speed over broad coverage.");
        assumptions.add(normalized.contains("software")
                ? "- Delivery capacity should be protected from custom work sprawl."
                : "- The first offer should be constrained tightly enough to estimate profitably.");
        return assumptions;
    }
}
