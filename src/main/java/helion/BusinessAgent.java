package helion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class BusinessAgent {
    private final LlmProvider manager;
    private final WorkerPool workerPool;
    private final BrowserTool browserTool;
    private final KnowledgeBase knowledgeBase;
    private final MultiDirectoryCorpus companyDataCorpus;
    private final CompanyDataSources companyDataSources;
    private final AgentRegistry agentRegistry;
    private final UsageTracker usageTracker;
    private final AgentDistiller distiller;
    private final MemoryStore memoryStore;
    private final EmailDraftStore emailDraftStore;
    private final ProspectStore prospectStore;
    private final ProspectSearchQueueStore prospectSearchQueueStore;
    private final HelionConfig config;

    public BusinessAgent(LlmProvider manager, WorkerPool workerPool, BrowserTool browserTool, KnowledgeBase knowledgeBase, MultiDirectoryCorpus companyDataCorpus, CompanyDataSources companyDataSources, AgentRegistry agentRegistry, UsageTracker usageTracker, MemoryStore memoryStore, EmailDraftStore emailDraftStore, HelionConfig config) {
        this.manager = manager;
        this.workerPool = workerPool;
        this.browserTool = browserTool;
        this.knowledgeBase = knowledgeBase;
        this.companyDataCorpus = companyDataCorpus;
        this.companyDataSources = companyDataSources;
        this.agentRegistry = agentRegistry;
        this.usageTracker = usageTracker;
        this.distiller = new AgentDistiller(manager, knowledgeBase, companyDataCorpus);
        this.memoryStore = memoryStore;
        this.emailDraftStore = emailDraftStore;
        this.prospectStore = new ProspectStore(agentRegistry);
        this.prospectSearchQueueStore = new ProspectSearchQueueStore(agentRegistry);
        this.config = config;
    }

    public String respond(AgentRequest request) throws IOException, InterruptedException {
        if ("demo".equals(manager.name())) {
            String finalAnswer = manager.complete(buildSystemPrompt(request.mode()), buildUserPrompt(request)).trim();
            persistFinalAnswer(request, finalAnswer);
            return finalAnswer;
        }

        List<ToolObservation> observations = new ArrayList<>();
        observations.add(loadAgentContext(request.agentId()));
        observations.add(loadKnowledgeContext());
        observations.add(loadCompanyDataContext());
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
                - Use the agent role and distilled observations to stay within the selected agent's job.
                - Use the knowledge observation as the source of truth for what the business sells, who it serves, and which pains it solves.
                - Use company data when internal documents are relevant, but do not treat raw company data as cleaner than curated agent-distilled notes.
                - Use WORKER for focused subtasks that benefit from a separate model pass.
                - Use FINAL when you have enough information and follow the schema exactly.
                - Do not include any text outside the action block.
                """;
    }

    private String buildUserPrompt(AgentRequest request) {
        String agentId = request.agentId() == null || request.agentId().isBlank() ? "default" : request.agentId();
        return "Agent: " + agentId + "\nMode: " + request.mode().name().toLowerCase() + "\nBusiness request:\n" + request.prompt();
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
        prompt.append("- Agents dir: ").append(agentRegistry.describe()).append('\n');
        prompt.append("- Knowledge: ").append(knowledgeBase.describe()).append('\n');
        prompt.append("- Company data: ").append(companyDataCorpus.describe()).append('\n');
        prompt.append("- Company data sources file: ").append(companyDataSources.sourcesFile()).append('\n');
        prompt.append("- Usage events file: ").append(config.usageEventsFile()).append('\n');
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

    private String buildProspectingSystemPrompt(int count) {
        return """
                You are Helion running a prospect collection workflow for the prospecting agent.
                Your job is to find plausible buyer companies and, when supported by public evidence, likely customer contacts.

                Use the existing action protocol and reply with exactly one action block:

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
                KEY: market-notes

                ACTION: WRITE_MEMORY
                KEY: market-notes
                CONTENT:
                reusable note

                ACTION: FINAL
                CONTENT:
                PROSPECT:
                Company: company name
                Website: https://company.example
                Contact Name: full name or blank
                Contact Role: likely buyer role
                Contact Email: public email or blank
                Phone: public phone or blank
                Location: city / region / country
                Industry: short industry description
                Fit Score: high|medium|low
                Status: new
                Priority: high|medium|low
                Owner: unassigned
                Discovered At: YYYY-MM-DD or blank
                Last Updated: YYYY-MM-DD or blank
                Tags:
                - off-road
                - chassis
                Why Fit: one concise fit explanation
                Evidence: one concise evidence summary
                Source URLs:
                - https://source-one.example
                - https://source-two.example
                Next Action: what to do next
                <<<END_PROSPECT>>>

                Rules:
                - Return about %d prospects.
                - Use the prospecting agent role and distilled context as the filter for fit.
                - Use web search and page fetches to validate the company and contact details.
                - Do not invent contact names or email addresses; leave them blank if unsupported.
                - Prefer companies with evidence of repeated tube fabrication, welded tube assemblies, frames, chassis, cages, racks, or similar work.
                - Prioritize quality over quantity.
                - Do not include commentary outside action blocks.
                """.formatted(Math.max(1, count));
    }

    private String buildProspectingUserPrompt(String searchFocus, int count, List<ToolObservation> observations, int turn) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Prospecting objective:\n");
        prompt.append(searchFocus).append('\n').append('\n');
        prompt.append("Target number of prospects: ").append(Math.max(1, count)).append('\n');
        prompt.append("Turn: ").append(turn).append(" / ").append(config.maxTurns()).append('\n');
        prompt.append("Browser enabled: ").append(browserTool.isEnabled()).append('\n');
        prompt.append('\n');
        prompt.append("Observations\n");
        for (int i = 0; i < observations.size(); i++) {
            prompt.append("Observation ").append(i + 1).append(":\n");
            prompt.append(observations.get(i).render()).append('\n').append('\n');
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

    private ToolObservation loadKnowledgeContext() throws IOException {
        return new ToolObservation("knowledge", "business-context", knowledgeBase.loadContext());
    }

    private ToolObservation loadCompanyDataContext() throws IOException {
        return new ToolObservation("company_data", "shared-documents", companyDataCorpus.loadContext());
    }

    private ToolObservation loadAgentContext(String agentId) throws IOException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            return new ToolObservation("agent", "selection", "No specific agent selected.");
        }

        String role = readIfExists(profile.roleFile());
        AgentStatus status = AgentStatus.parse(readIfExists(profile.statusFile()), config);
        String distilled = new DirectoryCorpus(true, profile.distilledDir(), 8000, 3).loadContext();
        String workspace = new DirectoryCorpus(true, profile.workspaceDir(), 8000, 3).loadContext();

        StringBuilder out = new StringBuilder();
        out.append("Agent ID: ").append(profile.id()).append('\n');
        out.append("Role:\n").append(role.isBlank() ? "No role.md content." : role).append('\n').append('\n');
        out.append("Status:\n").append(status.renderedStatus()).append('\n').append('\n');
        out.append("Distilled context:\n").append(distilled).append('\n').append('\n');
        out.append("Workspace context:\n").append(workspace);
        return new ToolObservation("agent", profile.id(), out.toString().trim());
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
        String agentPrefix = request.agentId() == null || request.agentId().isBlank() ? "" : request.agentId().trim().toLowerCase() + "-";
        return agentPrefix + request.mode().name().toLowerCase() + "-" + compact;
    }

    private String formatFinalAnswer(String raw) {
        FinalResponse parsed = FinalResponseParser.parse(raw);
        return parsed.render();
    }

    public String emailConfigReport() {
        return emailDraftStore.describe();
    }

    public String saveEmailDraft(String agentId, String to, String subject, String body, String notes) throws IOException {
        return emailDraftStore.saveDraft(agentId, to, subject, body, notes);
    }

    public String collectProspects(String agentId, String searchFocus, int count) throws IOException, InterruptedException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            return "Unknown agent: " + agentId;
        }
        if ("demo".equals(manager.name())) {
            List<ProspectRecord> demoRecords = List.of(
                    new ProspectRecord(
                            "Example Fabrication Co.",
                            "https://example.com",
                            "",
                            "Production Manager",
                            "",
                            "",
                            "Unknown",
                            "Tube fabrication",
                            "medium",
                            "new",
                            "medium",
                            "unassigned",
                            java.time.LocalDate.now().toString(),
                            java.time.LocalDate.now().toString(),
                            List.of("demo", "placeholder"),
                            "Likely does repeat tube fabrication work based on the requested search focus.",
                            "Demo mode cannot browse live web results, so this is a placeholder prospect.",
                            List.of(),
                            "Run prospecting with a live manager and browser enabled to gather real contacts."));
            return prospectStore.save(agentId, searchFocus, demoRecords);
        }

        AgentRequest request = new AgentRequest(AgentMode.ANALYZE, searchFocus, agentId);
        List<ToolObservation> observations = new ArrayList<>();
        observations.add(loadAgentContext(agentId));
        observations.add(loadKnowledgeContext());
        observations.add(loadCompanyDataContext());
        observations.add(loadAutomaticMemoryContext(request));
        for (int turn = 1; turn <= config.maxTurns(); turn++) {
            String systemPrompt = buildProspectingSystemPrompt(count);
            String userPrompt = buildProspectingUserPrompt(searchFocus, count, observations, turn);
            ManagerAction action = ManagerActionParser.parse(manager.complete(systemPrompt, userPrompt));

            switch (action) {
                case FinalAction finalAction -> {
                    List<ProspectRecord> records = ProspectParser.parse(finalAction.content());
                    if (records.isEmpty()) {
                        return "No structured prospects were returned. Try a narrower search focus.";
                    }
                    return prospectStore.save(agentId, searchFocus, records);
                }
                case WorkerAction workerAction -> observations.add(runWorker(workerAction, request));
                case SearchAction searchAction -> observations.add(runSearch(searchAction));
                case FetchAction fetchAction -> observations.add(runFetch(fetchAction));
                case ReadMemoryAction readMemoryAction -> observations.add(runMemoryRead(readMemoryAction));
                case WriteMemoryAction writeMemoryAction -> observations.add(runMemoryWrite(writeMemoryAction));
            }
        }
        return "Prospecting did not complete within " + config.maxTurns() + " turns.";
    }

    public String collectQueuedProspects(String agentId, int count) throws IOException, InterruptedException {
        ProspectSearchQueueItem item = prospectSearchQueueStore.nextDue(agentId);
        if (item == null) {
            return "No due prospecting queue items.";
        }
        String result = collectProspects(agentId, item.effectiveQuery(), count);
        int resultsSeen = extractSavedCount(result);
        prospectSearchQueueStore.markRun(agentId, item.afterRun(item.resultsSeen() + resultsSeen));
        return result + "\nQueue item advanced: " + item.summaryLine() + " -> pass " + item.afterRun(item.resultsSeen() + resultsSeen).pass();
    }

    public String addProspectQueueItem(String agentId, String queryTemplate, String region, String city, String industry, String notes) throws IOException {
        return prospectSearchQueueStore.add(agentId, queryTemplate, region, city, industry, notes);
    }

    public String prospectQueueReport(String agentId) throws IOException {
        return prospectSearchQueueStore.renderList(agentId);
    }

    private int extractSavedCount(String result) {
        if (result == null || result.isBlank()) {
            return 0;
        }
        String prefix = "Saved ";
        int start = result.indexOf(prefix);
        if (start < 0) {
            return 0;
        }
        int numberStart = start + prefix.length();
        int numberEnd = numberStart;
        while (numberEnd < result.length() && Character.isDigit(result.charAt(numberEnd))) {
            numberEnd++;
        }
        if (numberEnd == numberStart) {
            return 0;
        }
        try {
            return Integer.parseInt(result.substring(numberStart, numberEnd));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public String usageReport() throws IOException {
        return usageTracker.renderText();
    }

    public String distillAgent(String agentId) throws IOException, InterruptedException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            return "Unknown agent: " + agentId;
        }
        List<DistilledFile> files = distiller.distill(profile);
        distiller.writeFiles(profile, files);

        StringBuilder out = new StringBuilder();
        out.append("Distillation complete for agent ").append(profile.id()).append('\n');
        out.append("Wrote files:\n");
        for (DistilledFile file : files) {
            out.append("- ").append(file.name()).append('\n');
        }
        return out.toString().trim();
    }

    private String readIfExists(java.nio.file.Path file) throws IOException {
        if (file == null || java.nio.file.Files.exists(file) == false) {
            return "";
        }
        return java.nio.file.Files.readString(file).trim();
    }
}
