package helion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class BusinessAgent {
    private final LlmProvider manager;
    private final WorkerPool workerPool;
    private final BrowserTool browserTool;
    private final MemoryStore memoryStore;
    private final HelionConfig config;

    public BusinessAgent(LlmProvider manager, WorkerPool workerPool, BrowserTool browserTool, MemoryStore memoryStore, HelionConfig config) {
        this.manager = manager;
        this.workerPool = workerPool;
        this.browserTool = browserTool;
        this.memoryStore = memoryStore;
        this.config = config;
    }

    public String respond(AgentRequest request) throws IOException, InterruptedException {
        if ("demo".equals(manager.name())) {
            String finalAnswer = manager.complete(buildSystemPrompt(request.mode()), buildUserPrompt(request)).trim();
            persistFinalAnswer(request, finalAnswer);
            return finalAnswer;
        }

        List<ToolObservation> observations = new ArrayList<>();
        observations.add(loadAutomaticMemoryContext(request));
        for (int turn = 1; turn <= config.maxTurns(); turn++) {
            String systemPrompt = buildManagerSystemPrompt(request.mode());
            String userPrompt = buildManagerUserPrompt(request, observations, turn);
            ManagerAction action = ManagerActionParser.parse(manager.complete(systemPrompt, userPrompt));

            switch (action) {
                case FinalAction finalAction -> {
                    String finalAnswer = formatFinalAnswer(finalAction.content());
                    persistFinalAnswer(request, finalAnswer);
                    return finalAnswer;
                }
                case WorkerAction workerAction -> observations.add(runWorker(workerAction, request));
                case SearchAction searchAction -> observations.add(runSearch(searchAction));
                case FetchAction fetchAction -> observations.add(runFetch(fetchAction));
                case ReadMemoryAction readMemoryAction -> observations.add(runMemoryRead(readMemoryAction));
                case WriteMemoryAction writeMemoryAction -> observations.add(runMemoryWrite(writeMemoryAction));
            }
        }

        String fallbackPrompt = buildFallbackPrompt(request, observations);
        String finalAnswer = formatFinalAnswer(manager.complete(buildSystemPrompt(request.mode()), fallbackPrompt).trim());
        persistFinalAnswer(request, finalAnswer);
        return finalAnswer;
    }

    private String buildSystemPrompt(AgentMode mode) {
        String shared = """
                You are Helion, a pragmatic business agent.
                Give concise, decision-oriented business advice.
                Prefer structured sections, concrete assumptions, and actionable next steps.
                Avoid hype and avoid generic filler.
                """;

        return switch (mode) {
            case PLAN -> shared + """
                    Focus on execution planning.
                    Include: objective, assumptions, milestones, risks, and next 3 actions.
                    """;
            case ANALYZE -> shared + """
                    Focus on diagnosis and recommendations.
                    Include: current problem, likely causes, opportunities, and prioritized fixes.
                    """;
            case EMAIL -> shared + """
                    Write business communication.
                    Include a polished subject line and a concise body.
                    """;
            case GENERAL -> shared + """
                    Help with strategy, operations, growth, finance framing, or messaging as relevant.
                    """;
        };
    }

    private String buildManagerSystemPrompt(AgentMode mode) {
        return buildSystemPrompt(mode) + """

                You are operating as the manager in a manager-worker agent system.
                You may delegate research or analysis tasks to workers and you may use browser tools.
                Reply using exactly one action block in one of these formats:

                ACTION: WORKER
                TITLE: short label
                PROMPT:
                detailed worker task

                ACTION: SEARCH
                QUERY: search terms
                LIMIT: 5

                ACTION: FETCH
                URL: https://example.com/page

                ACTION: READ_MEMORY
                KEY: customer-acme

                ACTION: WRITE_MEMORY
                KEY: customer-acme
                CONTENT:
                memory note to persist

                ACTION: FINAL
                CONTENT:
                STATUS: ready
                TITLE: short answer title
                SUMMARY:
                concise executive summary
                DETAILS:
                main answer body
                NEXT_STEPS:
                - first action
                - second action
                SOURCES:
                - source or note

                Rules:
                - Use SEARCH when current information from the web would materially improve the answer.
                - Use FETCH after SEARCH when you need details from a specific source.
                - Use READ_MEMORY to retrieve prior project, customer, or market notes.
                - Use WRITE_MEMORY to save durable notes worth reusing later.
                - Use WORKER for focused subtasks that benefit from a separate model pass.
                - Use FINAL when you have enough information and follow the schema exactly.
                - Do not include any text outside the action block.
                """;
    }

    private String buildUserPrompt(AgentRequest request) {
        return "Mode: " + request.mode().name().toLowerCase() + "\nBusiness request:\n" + request.prompt();
    }

    private String buildManagerUserPrompt(AgentRequest request, List<ToolObservation> observations, int turn) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Mode: ").append(request.mode().name().toLowerCase()).append('\n');
        prompt.append("Business request:\n").append(request.prompt()).append('\n');
        prompt.append('\n');
        prompt.append("Runtime\n");
        prompt.append("- Turn: ").append(turn).append(" / ").append(config.maxTurns()).append('\n');
        prompt.append("- Manager provider: ").append(manager.name()).append('\n');
        prompt.append("- Worker count: ").append(workerPool.size()).append('\n');
        prompt.append("- Browser enabled: ").append(browserTool.isEnabled()).append('\n');
        prompt.append("- Memory: ").append(memoryStore.describe()).append('\n');
        prompt.append('\n');
        prompt.append("Observations\n");
        if (observations.isEmpty()) {
            prompt.append("None yet.\n");
        } else {
            for (int i = 0; i < observations.size(); i++) {
                prompt.append("Observation ").append(i + 1).append(":\n");
                prompt.append(observations.get(i).render()).append('\n').append('\n');
            }
        }
        return prompt.toString().trim();
    }

    private ToolObservation runWorker(WorkerAction action, AgentRequest request) throws IOException, InterruptedException {
        String workerPrompt = """
                Original business request:
                %s

                Worker task:
                %s

                Return concise, high-signal findings only. State assumptions when necessary.
                """.formatted(request.prompt(), action.prompt());
        String result = workerPool.run(action.title(), workerPrompt);
        return new ToolObservation("worker", action.title(), result);
    }

    private ToolObservation runSearch(SearchAction action) throws IOException, InterruptedException {
        BrowserSearchResult result = browserTool.search(action.query(), action.limit());
        return new ToolObservation("search", action.query(), result.render());
    }

    private ToolObservation runFetch(FetchAction action) throws IOException, InterruptedException {
        BrowserPage page = browserTool.fetch(action.url());
        return new ToolObservation("fetch", action.url(), page.render());
    }

    private ToolObservation runMemoryRead(ReadMemoryAction action) throws IOException {
        String key = action.key().isBlank() ? "general" : action.key();
        return new ToolObservation("read_memory", key, memoryStore.read(key));
    }

    private ToolObservation runMemoryWrite(WriteMemoryAction action) throws IOException {
        String key = action.key().isBlank() ? "general" : action.key();
        memoryStore.append(key, action.content());
        return new ToolObservation("write_memory", key, "Saved memory note.");
    }

    private String buildFallbackPrompt(AgentRequest request, List<ToolObservation> observations) {
        StringBuilder builder = new StringBuilder();
        builder.append(buildUserPrompt(request)).append('\n').append('\n');
        builder.append("Observed notes:\n");
        for (ToolObservation observation : observations) {
            builder.append(observation.render()).append('\n').append('\n');
        }
        builder.append("""
                Provide the best final answer now and use this schema exactly:
                STATUS: ready
                TITLE: short answer title
                SUMMARY:
                concise executive summary
                DETAILS:
                main answer body
                NEXT_STEPS:
                - first action
                SOURCES:
                - source or note
                """);
        return builder.toString();
    }

    private ToolObservation loadAutomaticMemoryContext(AgentRequest request) throws IOException {
        if (!memoryStore.isEnabled()) {
            return new ToolObservation("memory", "availability", "Memory disabled.");
        }
        List<String> keys = memoryStore.listKeys();
        StringBuilder builder = new StringBuilder();
        builder.append("Available keys: ");
        builder.append(keys.isEmpty() ? "none" : String.join(", ", keys));
        builder.append('\n');
        builder.append("Suggested key for this request: ").append(suggestMemoryKey(request)).append('\n');
        return new ToolObservation("memory", "availability", builder.toString().trim());
    }

    private void persistFinalAnswer(AgentRequest request, String finalAnswer) throws IOException {
        if (!memoryStore.isEnabled()) {
            return;
        }
        String key = suggestMemoryKey(request);
        String summary = "Request: " + request.prompt() + "\n\nFinal answer summary:\n" + TextUtils.limit(finalAnswer, 1800);
        memoryStore.append(key, summary);
    }

    private String suggestMemoryKey(AgentRequest request) {
        String prompt = request.prompt() == null ? "" : request.prompt();
        String compact = prompt.length() > 64 ? prompt.substring(0, 64) : prompt;
        return request.mode().name().toLowerCase() + "-" + compact;
    }

    private String formatFinalAnswer(String raw) {
        FinalResponse parsed = FinalResponseParser.parse(raw);
        return parsed.render();
    }
}
