package helion;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final EmailInboxStore emailInboxStore;
    private final ProspectStore prospectStore;
    private final ProspectSearchQueueStore prospectSearchQueueStore;
    private final SocialOpportunityStore socialOpportunityStore;
    private final SocialSearchQueueStore socialSearchQueueStore;
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
        this.emailInboxStore = new EmailInboxStore(config.emailSettings(), agentRegistry);
        this.prospectStore = new ProspectStore(agentRegistry, config);
        this.prospectSearchQueueStore = new ProspectSearchQueueStore(agentRegistry);
        this.socialOpportunityStore = new SocialOpportunityStore(agentRegistry, config);
        this.socialSearchQueueStore = new SocialSearchQueueStore(agentRegistry);
        this.config = config;
    }

    public String respond(AgentRequest request) throws IOException, InterruptedException {
        return withUsageAgent(request.agentId(), () -> respond(request, true));
    }

    public String respondPlain(AgentRequest request) throws IOException, InterruptedException {
        return withUsageAgent(request.agentId(), () -> respond(request, false));
    }

    private String respond(AgentRequest request, boolean styledOutput) throws IOException, InterruptedException {
        String grounded = groundedLightweightResponse(request, styledOutput);
        if (!grounded.isBlank()) {
            return grounded;
        }
        LlmProvider coordinator = coordinatorProvider(request.agentId());
        if ("demo".equals(coordinator.name())) {
            String finalAnswer = coordinator.complete(buildSystemPrompt(request.mode()), buildUserPrompt(request)).trim();
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
            String userPrompt = buildManagerUserPrompt(request, observations, turn, coordinator);
            ManagerAction action = ManagerActionParser.parse(coordinator.complete(systemPrompt, userPrompt));

            switch (action) {
                case FinalAction finalAction -> {
                    String finalAnswer = formatFinalAnswer(finalAction.content(), styledOutput);
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
        String finalAnswer = formatFinalAnswer(coordinator.complete(buildSystemPrompt(request.mode()), fallbackPrompt).trim(), styledOutput);
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
                - Treat the selected agent's primary output file as the canonical deliverable you are helping improve.
                - Use WORKER for focused subtasks that benefit from a separate model pass.
                - Use FINAL when you have enough information and follow the schema exactly.
                - Do not include any text outside the action block.
                """;
    }

    private String buildUserPrompt(AgentRequest request) {
        String agentId = request.agentId() == null || request.agentId().isBlank() ? "default" : request.agentId();
        return "Agent: " + agentId + "\nMode: " + request.mode().name().toLowerCase() + "\nBusiness request:\n" + request.prompt();
    }

    private String buildManagerUserPrompt(AgentRequest request, List<ToolObservation> observations, int turn, LlmProvider coordinator) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Mode: ").append(request.mode().name().toLowerCase()).append('\n');
        prompt.append("Business request:\n").append(request.prompt()).append('\n');
        prompt.append('\n');
        prompt.append("Runtime\n");
        prompt.append("- Turn: ").append(turn).append(" / ").append(config.maxTurns()).append('\n');
        prompt.append("- Coordinator provider: ").append(coordinator.name()).append('\n');
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
                - Treat the prospecting agent's primary output file as the main deliverable this workflow is updating.
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

    private String buildSocialMediaSystemPrompt(int count) {
        return """
                You are Helion running a social-media opportunity discovery workflow.
                Your job is to find public social or forum conversations where Arma Automotive's CNC tube notcher could be relevant.

                Focus on public indexed pages first, especially Reddit and public forums. Do not invent conversations.

                ACTION: WORKER
                TITLE: short label
                PROMPT:
                detailed worker task

                ACTION: SEARCH
                QUERY: search terms
                LIMIT: 10

                ACTION: FETCH
                URL: https://example.com/thread

                ACTION: FINAL
                CONTENT:
                OPPORTUNITY:
                Title: thread title
                URL: https://thread.example
                Site: reddit.com
                Community: subreddit/forum name or blank
                Author: public handle or blank
                Posted: public date or blank
                Relevance: high|medium|low
                Buyer Signal: one concise explanation of why this looks like a fit
                Product Fit: one concise explanation of how the CNC tube notcher fits
                Recommended Angle: one concise non-spam reply or outreach angle
                Evidence: one concise evidence summary from the public thread or snippet
                Status: new
                Tags:
                - reddit
                - fabrication
                Source URLs:
                - https://thread.example
                <<<END_OPPORTUNITY>>>

                Rules:
                - Return about %d opportunities.
                - Use public conversation URLs and snippets as grounding.
                - Prefer threads about tube notching, tube coping, roll cages, tube chassis, fabrication bottlenecks, or tool recommendations.
                - Do not invent authors, communities, or dates if they are not visible.
                - Treat the social-media agent's primary output file as the main deliverable this workflow is updating.
                - Do not include commentary outside action blocks.
                """.formatted(Math.max(1, count));
    }

    private String buildSocialMediaUserPrompt(String searchFocus, int count, List<ToolObservation> observations, int turn) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Social opportunity objective:\n");
        prompt.append(searchFocus).append('\n').append('\n');
        prompt.append("Target number of opportunities: ").append(Math.max(1, count)).append('\n');
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
        String result = workerPool.run(action.title(), workerPrompt, preferredLocalPool(request.agentId()));
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
        String primaryOutput = loadPrimaryOutput(profile, status);

        StringBuilder out = new StringBuilder();
        out.append("Agent ID: ").append(profile.id()).append('\n');
        out.append("Role:\n").append(role.isBlank() ? "No role.md content." : role).append('\n').append('\n');
        out.append("Status:\n").append(status.renderedStatus()).append('\n').append('\n');
        if (!status.primaryOutputFile().isBlank()) {
            out.append("Primary output file: ").append(status.primaryOutputFile()).append('\n');
            out.append("Primary output content:\n").append(primaryOutput.isBlank() ? "(empty)" : primaryOutput).append('\n').append('\n');
        }
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

    private String formatFinalAnswer(String raw, boolean styledOutput) {
        FinalResponse parsed = FinalResponseParser.parse(raw);
        return styledOutput ? parsed.render() : parsed.renderPlain();
    }

    public String emailConfigReport() {
        return emailDraftStore.describe();
    }

    public String saveEmailDraft(String agentId, String to, String subject, String body, String notes) throws IOException {
        return emailDraftStore.saveDraft(agentId, to, subject, body, notes);
    }

    public String syncAndDraftEmailInbox(String agentId, int syncLimit, int draftLimit) throws IOException, InterruptedException {
        String syncResult = syncEmailInbox(agentId, syncLimit);
        String draftResult = generateEmailDraftsFromInbox(agentId, draftLimit);
        return syncResult + "\n" + draftResult;
    }

    public String syncEmailInbox(String agentId, int limit) throws IOException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown agent: " + agentId);
        }
        AgentActivityStore activityStore = new AgentActivityStore();
        logAgentActivity(
                activityStore,
                profile,
                "info",
                "email-sync",
                "Starting inbox sync",
                "Agent: " + agentId + "\nLimit: " + Math.max(1, limit) + "\nIMAP host: " + nonBlank(config.emailSettings().imapHost()));
        try {
            String result = emailInboxStore.syncInbox(agentId, limit);
            logAgentActivity(
                    activityStore,
                    profile,
                    "success",
                    "email-sync",
                    "Inbox sync completed",
                    result);
            return result;
        } catch (IOException ex) {
            logAgentActivity(
                    activityStore,
                    profile,
                    "error",
                    "email-sync",
                    "Inbox sync failed",
                    "Agent: " + agentId + "\nLimit: " + Math.max(1, limit) + "\nIMAP host: " + nonBlank(config.emailSettings().imapHost()) + "\nError: " + nonBlank(ex.getMessage()));
            throw ex;
        }
    }

    public String generateEmailDraftsFromInbox(String agentId, int limit) throws IOException, InterruptedException {
        return withUsageAgent(agentId, () -> generateEmailDraftsFromInboxInternal(agentId, limit));
    }

    private String generateEmailDraftsFromInboxInternal(String agentId, int limit) throws IOException, InterruptedException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            return "Unknown agent: " + agentId;
        }
        Path inboxFile = profile.workspaceDir().resolve("inbox_summary.md");
        String inboxSummary = readIfExists(inboxFile);
        List<EmailInboxSummaryMessage> messages = EmailInboxSummaryParser.parse(inboxSummary);
        if (messages.isEmpty()) {
            return "No inbox messages available for drafting.";
        }

        String existingDrafts = readIfExists(AgentOutputResolver.resolvePrimaryOutputFile(profile, config, "workspace/reply_drafts.md"));
        List<EmailInboxSummaryMessage> actionable = new ArrayList<>();
        for (EmailInboxSummaryMessage message : messages) {
            if (!isExternalInboxMessage(message)) {
                continue;
            }
            if (alreadyDrafted(existingDrafts, message.sequenceNumber())) {
                continue;
            }
            actionable.add(message);
            if (actionable.size() >= Math.max(1, limit)) {
                break;
            }
        }
        if (actionable.isEmpty()) {
            return "No new inbox messages need draft replies.";
        }

        AgentActivityStore activityStore = new AgentActivityStore();
        LlmProvider coordinator = coordinatorProvider(agentId);
        String role = readIfExists(profile.roleFile());
        String distilled = new DirectoryCorpus(true, profile.distilledDir(), 5000, 4).loadContext();
        String businessContext = TextUtils.limit(knowledgeBase.loadContext(), 5000);
        int drafted = 0;

        for (EmailInboxSummaryMessage message : actionable) {
            String to = firstEmailAddress(message.from());
            String subject = replySubject(message.subject());
            String prompt = """
                    Role:
                    %s

                    Distilled email-support context:
                    %s

                    Business context:
                    %s

                    Customer message:
                    - From: %s
                    - Subject: %s
                    - Date: %s
                    - Preview: %s

                    Write a concise draft reply email body for Arma Automotive.
                    Rules:
                    - Be practical and polite.
                    - If the inquiry is not a fit for the business, say so clearly and briefly.
                    - Do not invent technical specs or commitments.
                    - Return only the email body, no headings or commentary.
                    """.formatted(
                    role,
                    distilled,
                    businessContext,
                    message.from(),
                    message.subject(),
                    message.date(),
                    message.preview());
            try {
                String body = coordinator.complete(buildSystemPrompt(AgentMode.EMAIL), prompt).trim();
                String notes = "Auto-drafted from inbox message " + message.sequenceNumber();
                emailDraftStore.saveDraft(agentId, to, subject, body, notes);
                drafted++;
                logAgentActivity(
                        activityStore,
                        profile,
                        "success",
                        "email-draft",
                        "Drafted reply for inbox message " + message.sequenceNumber(),
                        "To: " + nonBlank(to) + "\nSubject: " + nonBlank(subject) + "\nFrom: " + nonBlank(message.from()) + "\nPreview: " + TextUtils.limit(message.preview(), 500));
            } catch (IOException ex) {
                logAgentActivity(
                        activityStore,
                        profile,
                        "error",
                        "email-draft",
                        "Draft generation failed for inbox message " + message.sequenceNumber(),
                        "From: " + nonBlank(message.from()) + "\nSubject: " + nonBlank(message.subject()) + "\nError: " + nonBlank(ex.getMessage()));
                throw ex;
            }
        }
        return "Drafted " + drafted + " email repl" + (drafted == 1 ? "y." : "ies.");
    }

    public String collectProspects(String agentId, String searchFocus, int count) throws IOException, InterruptedException {
        return collectProspects(agentId, searchFocus, count, "cloud");
    }

    public String collectProspects(String agentId, String searchFocus, int count, String executionTarget) throws IOException, InterruptedException {
        return withUsageAgent(agentId, () -> collectProspectsInternal(agentId, searchFocus, count, executionTarget));
    }

    private String collectProspectsInternal(String agentId, String searchFocus, int count, String executionTarget) throws IOException, InterruptedException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            return "Unknown agent: " + agentId;
        }
        AgentActivityStore activityStore = new AgentActivityStore();
        LlmProvider autonomousProvider = autonomousProvider(executionTarget, preferredLocalPool(agentId));
        if ("demo".equals(autonomousProvider.name())) {
            logAgentActivity(activityStore, profile, "info", "prospecting", "Demo mode prospecting run", "Search focus: " + searchFocus);
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
            String saved = prospectStore.save(agentId, searchFocus, demoRecords);
            logAgentActivity(activityStore, profile, "success", "prospecting", "Saved demo prospect output", saved);
            return saved;
        }

        AgentRequest request = new AgentRequest(AgentMode.ANALYZE, searchFocus, agentId);
        List<ToolObservation> observations = new ArrayList<>();
        observations.add(loadAgentContext(agentId));
        observations.add(loadKnowledgeContext());
        observations.add(loadCompanyDataContext());
        observations.add(loadAutomaticMemoryContext(request));
        boolean hasGroundedWebEvidence = false;
        logAgentActivity(
                activityStore,
                profile,
                "info",
                "prospecting",
                "Started prospecting workflow",
                "Search focus: " + searchFocus + "\nExecution target: " + executionTarget + "\nRequested prospect count: " + count);
        for (int turn = 1; turn <= config.maxTurns(); turn++) {
            String systemPrompt = buildProspectingSystemPrompt(count);
            String userPrompt = buildProspectingUserPrompt(searchFocus, count, observations, turn);
            ManagerAction action;
            try {
                action = ManagerActionParser.parse(autonomousProvider.complete(systemPrompt, userPrompt));
            } catch (IOException ex) {
                        logProspectingFailure(
                                activityStore,
                                profile,
                                "llm",
                                "Coordinator request failed",
                                "Turn: " + turn + "\nProvider: " + autonomousProvider.name() + "\nError: " + nonBlank(ex.getMessage()));
                        throw ex;
            }
            logAgentActivity(
                    activityStore,
                    profile,
                    "info",
                    "prospecting-turn",
                    "Turn " + turn + " selected " + actionName(action),
                    prospectingActionDetails(action));

            switch (action) {
                case FinalAction finalAction -> {
                    List<ProspectRecord> records = ProspectParser.parse(finalAction.content());
                    if (records.isEmpty()) {
                        logAgentActivity(
                                activityStore,
                                profile,
                                "error",
                                "prospecting-final",
                                "No structured prospects returned",
                                TextUtils.limit(finalAction.content(), 1200));
                        return "No structured prospects were returned. Try a narrower search focus.";
                    }
                    if (!hasGroundedWebEvidence) {
                        logAgentActivity(
                                activityStore,
                                profile,
                                "error",
                                "prospecting-final",
                                "Blocked ungrounded prospect output",
                                "The model returned prospects without any grounded search or fetch evidence.\n\n"
                                        + TextUtils.limit(finalAction.content(), 1200));
                        return "Prospecting produced ungrounded output without live web evidence. No prospects were saved.";
                    }
                    List<ProspectRecord> groundedRecords = filterGroundedProspects(records);
                    if (groundedRecords.isEmpty()) {
                        logAgentActivity(
                                activityStore,
                                profile,
                                "error",
                                "prospecting-final",
                                "Blocked prospects with no source URLs",
                                TextUtils.limit(finalAction.content(), 1200));
                        return "Prospecting returned records without usable source URLs. No prospects were saved.";
                    }
                    String saved = prospectStore.save(agentId, searchFocus, groundedRecords);
                    logAgentActivity(
                            activityStore,
                            profile,
                            "success",
                            "prospecting-final",
                            "Saved " + groundedRecords.size() + " parsed prospects",
                            renderProspectSummary(groundedRecords) + "\n\n" + saved);
                    return saved;
                }
                case WorkerAction workerAction -> {
                    ToolObservation workerObservation;
                    try {
                        workerObservation = runWorker(workerAction, request);
                    } catch (IOException ex) {
                        logProspectingFailure(
                                activityStore,
                                profile,
                                "worker",
                                "Worker action failed: " + workerAction.title(),
                                "Turn: " + turn + "\nError: " + nonBlank(ex.getMessage()));
                        throw ex;
                    }
                    observations.add(workerObservation);
                    logAgentActivity(
                            activityStore,
                            profile,
                            "info",
                            "worker",
                            "Worker completed: " + workerAction.title(),
                            TextUtils.limit(workerObservation.content(), 1200));
                }
                case SearchAction searchAction -> {
                    BrowserSearchResult result;
                    try {
                        result = browserTool.search(searchAction.query(), searchAction.limit());
                    } catch (IOException ex) {
                        logProspectingFailure(
                                activityStore,
                                profile,
                                "search",
                                "Browser search failed",
                                "Turn: " + turn + "\nQuery: " + searchAction.query() + "\nError: " + nonBlank(ex.getMessage()));
                        throw ex;
                    }
                    observations.add(new ToolObservation("search", searchAction.query(), result.render()));
                    if (!result.items().isEmpty()) {
                        hasGroundedWebEvidence = true;
                    }
                    logAgentActivity(
                            activityStore,
                            profile,
                            "info",
                            "search",
                            "Found " + result.items().size() + " search results",
                            renderSearchActivity(result));
                }
                case FetchAction fetchAction -> {
                    BrowserPage page;
                    try {
                        page = browserTool.fetch(fetchAction.url());
                    } catch (IOException ex) {
                        logProspectingFailure(
                                activityStore,
                                profile,
                                "fetch",
                                "Browser fetch failed",
                                "Turn: " + turn + "\nURL: " + fetchAction.url() + "\nError: " + nonBlank(ex.getMessage()));
                        throw ex;
                    }
                    observations.add(new ToolObservation("fetch", fetchAction.url(), page.render()));
                    if (!page.content().isBlank()) {
                        hasGroundedWebEvidence = true;
                    }
                    logAgentActivity(
                            activityStore,
                            profile,
                            "info",
                            "fetch",
                            "Investigating " + fetchAction.url(),
                            renderFetchActivity(page));
                }
                case ReadMemoryAction readMemoryAction -> {
                    ToolObservation memoryObservation = runMemoryRead(readMemoryAction);
                    observations.add(memoryObservation);
                    logAgentActivity(
                            activityStore,
                            profile,
                            "info",
                            "memory-read",
                            "Loaded memory key " + readMemoryAction.key(),
                            TextUtils.limit(memoryObservation.content(), 1200));
                }
                case WriteMemoryAction writeMemoryAction -> {
                    ToolObservation memoryObservation = runMemoryWrite(writeMemoryAction);
                    observations.add(memoryObservation);
                    logAgentActivity(
                            activityStore,
                            profile,
                            "info",
                            "memory-write",
                            "Saved memory key " + writeMemoryAction.key(),
                            TextUtils.limit(writeMemoryAction.content(), 1200));
                }
            }
        }
        logAgentActivity(
                activityStore,
                profile,
                "error",
                "prospecting",
                "Prospecting turn limit reached",
                "Search focus: " + searchFocus + "\nMax turns: " + config.maxTurns());
        return "Prospecting did not complete within " + config.maxTurns() + " turns.";
    }

    private void logProspectingFailure(AgentActivityStore activityStore, AgentProfile profile, String task, String summary, String details) {
        logAgentActivity(activityStore, profile, "error", task, summary, details);
    }

    private String nonBlank(String value) {
        return value == null || value.isBlank() ? "(none)" : value;
    }

    private boolean isExternalInboxMessage(EmailInboxSummaryMessage message) {
        if (message == null) {
            return false;
        }
        String sender = firstEmailAddress(message.from()).toLowerCase();
        String supportAddress = config.emailSettings().address() == null ? "" : config.emailSettings().address().trim().toLowerCase();
        if (!supportAddress.isBlank() && sender.equals(supportAddress)) {
            return false;
        }
        return !sender.isBlank();
    }

    private boolean alreadyDrafted(String draftsContent, int sequenceNumber) {
        if (draftsContent == null || draftsContent.isBlank() || sequenceNumber < 0) {
            return false;
        }
        return draftsContent.contains("Auto-drafted from inbox message " + sequenceNumber);
    }

    private String firstEmailAddress(String value) {
        List<String> emails = extractEmails(value, 1);
        return emails.isEmpty() ? "" : emails.get(0);
    }

    private String replySubject(String subject) {
        String value = subject == null ? "" : subject.trim();
        if (value.isBlank()) {
            return "Re: Your message";
        }
        if (value.regionMatches(true, 0, "Re:", 0, 3)) {
            return value;
        }
        return "Re: " + value;
    }

    private List<ProspectRecord> filterGroundedProspects(List<ProspectRecord> records) {
        List<ProspectRecord> grounded = new ArrayList<>();
        for (ProspectRecord record : records) {
            if (record == null) {
                continue;
            }
            if (record.sourceUrls() == null || record.sourceUrls().isEmpty()) {
                continue;
            }
            boolean hasUsableUrl = false;
            for (String url : record.sourceUrls()) {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    hasUsableUrl = true;
                    break;
                }
            }
            if (hasUsableUrl) {
                grounded.add(record);
            }
        }
        return grounded;
    }

    public String collectQueuedProspects(String agentId, int count) throws IOException, InterruptedException {
        return collectQueuedProspects(agentId, count, "cloud");
    }

    public String collectQueuedProspects(String agentId, int count, String executionTarget) throws IOException, InterruptedException {
        return collectQueuedProspectsBatch(agentId, count, 1, executionTarget);
    }

    public String collectQueuedProspectsBatch(String agentId, int count, int maxItems, String executionTarget) throws IOException, InterruptedException {
        List<ProspectSearchQueueItem> dueItems = prospectSearchQueueStore.dueItems(agentId, Math.max(1, maxItems));
        if (dueItems.isEmpty()) {
            return "";
        }
        int delaySeconds = queueDelaySeconds(agentId);
        StringBuilder out = new StringBuilder();
        int processed = 0;
        for (ProspectSearchQueueItem item : dueItems) {
            if (processed > 0) {
                out.append("\n\n");
            }
            String result = collectProspects(agentId, item.effectiveQuery(), count, executionTarget);
            int resultsSeen = extractSavedCount(result);
            ProspectSearchQueueItem updated = item.afterRun(item.resultsSeen() + resultsSeen, delaySeconds);
            prospectSearchQueueStore.markRun(agentId, updated);
            out.append("Queue item ").append(item.id()).append(":\n");
            out.append(result).append('\n');
            out.append("Advanced: ").append(item.summaryLine()).append(" -> pass ").append(updated.pass());
            processed++;
        }
        out.append("\n\nProcessed ").append(processed).append(" queued prospect search");
        if (processed != 1) {
            out.append("es");
        }
        out.append(".");
        return out.toString().trim();
    }

    public String collectQueuedProspectsSingle(String agentId, int count, String executionTarget) throws IOException, InterruptedException {
        ProspectSearchQueueItem item = prospectSearchQueueStore.nextDue(agentId);
        if (item == null) {
            return "No due prospecting queue items.";
        }
        String result = collectProspects(agentId, item.effectiveQuery(), count, executionTarget);
        int resultsSeen = extractSavedCount(result);
        ProspectSearchQueueItem updated = item.afterRun(item.resultsSeen() + resultsSeen, queueDelaySeconds(agentId));
        prospectSearchQueueStore.markRun(agentId, updated);
        return result + "\nQueue item advanced: " + item.summaryLine() + " -> pass " + updated.pass();
    }

    public String addProspectQueueItem(String agentId, String queryTemplate, String region, String city, String industry, String notes) throws IOException {
        return prospectSearchQueueStore.add(agentId, queryTemplate, region, city, industry, notes);
    }

    public String prospectQueueReport(String agentId) throws IOException {
        return prospectSearchQueueStore.renderList(agentId);
    }

    public String collectSocialOpportunities(String agentId, String searchFocus, int count, String executionTarget) throws IOException, InterruptedException {
        return withUsageAgent(agentId, () -> collectSocialOpportunitiesInternal(agentId, searchFocus, count, executionTarget));
    }

    private String collectSocialOpportunitiesInternal(String agentId, String searchFocus, int count, String executionTarget) throws IOException, InterruptedException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            return "Unknown agent: " + agentId;
        }
        AgentActivityStore activityStore = new AgentActivityStore();
        LlmProvider autonomousProvider = autonomousProvider(executionTarget, preferredLocalPool(agentId));
        List<ToolObservation> observations = new ArrayList<>();
        AgentRequest request = new AgentRequest(AgentMode.ANALYZE, searchFocus, agentId);
        observations.add(loadAgentContext(agentId));
        observations.add(loadKnowledgeContext());
        observations.add(loadCompanyDataContext());
        boolean hasGroundedEvidence = false;

        logAgentActivity(
                activityStore,
                profile,
                "info",
                "social-media",
                "Started social opportunity workflow",
                "Search focus: " + searchFocus + "\nExecution target: " + executionTarget + "\nRequested opportunity count: " + count);

        for (int turn = 1; turn <= config.maxTurns(); turn++) {
            ManagerAction action;
            try {
                action = ManagerActionParser.parse(
                        autonomousProvider.complete(
                                buildSocialMediaSystemPrompt(count),
                                buildSocialMediaUserPrompt(searchFocus, count, observations, turn)));
            } catch (IOException ex) {
                logAgentActivity(activityStore, profile, "error", "llm", "Coordinator request failed",
                        "Turn: " + turn + "\nProvider: " + autonomousProvider.name() + "\nError: " + nonBlank(ex.getMessage()));
                throw ex;
            }

            logAgentActivity(
                    activityStore,
                    profile,
                    "info",
                    "social-turn",
                    "Turn " + turn + " selected " + actionName(action),
                    prospectingActionDetails(action));

            switch (action) {
                case FinalAction finalAction -> {
                    List<SocialOpportunityRecord> records = SocialOpportunityParser.parse(finalAction.content());
                    if (records.isEmpty()) {
                        logAgentActivity(activityStore, profile, "error", "social-final", "No structured opportunities returned",
                                TextUtils.limit(finalAction.content(), 1200));
                        return "No structured social opportunities were returned.";
                    }
                    if (!hasGroundedEvidence) {
                        logAgentActivity(activityStore, profile, "error", "social-final", "Blocked ungrounded social output",
                                TextUtils.limit(finalAction.content(), 1200));
                        return "Social-media produced ungrounded output without public search evidence. No opportunities were saved.";
                    }
                    List<SocialOpportunityRecord> groundedRecords = filterGroundedSocialOpportunities(records);
                    if (groundedRecords.isEmpty()) {
                        logAgentActivity(activityStore, profile, "error", "social-final", "Blocked social opportunities with no source URLs",
                                TextUtils.limit(finalAction.content(), 1200));
                        return "Social-media returned records without usable source URLs. No opportunities were saved.";
                    }
                    String saved = socialOpportunityStore.save(agentId, searchFocus, groundedRecords);
                    logAgentActivity(activityStore, profile, "success", "social-final",
                            "Saved " + groundedRecords.size() + " social opportunities",
                            renderSocialOpportunitySummary(groundedRecords) + "\n\n" + saved);
                    return saved;
                }
                case WorkerAction workerAction -> {
                    try {
                        ToolObservation workerObservation = runWorker(workerAction, request);
                        observations.add(workerObservation);
                        logAgentActivity(activityStore, profile, "info", "worker", "Worker completed: " + workerAction.title(),
                                TextUtils.limit(workerObservation.content(), 2000));
                    } catch (IOException ex) {
                        logAgentActivity(activityStore, profile, "error", "worker", "Worker action failed: " + workerAction.title(),
                                "Turn: " + turn + "\nError: " + nonBlank(ex.getMessage()));
                        throw ex;
                    }
                }
                case SearchAction searchAction -> {
                    BrowserSearchResult result;
                    try {
                        result = browserTool.search(searchAction.query(), searchAction.limit());
                    } catch (IOException ex) {
                        logAgentActivity(activityStore, profile, "error", "search", "Browser search failed",
                                "Turn: " + turn + "\nQuery: " + searchAction.query() + "\nError: " + nonBlank(ex.getMessage()));
                        throw ex;
                    }
                    observations.add(new ToolObservation("search", searchAction.query(), result.render()));
                    if (!result.items().isEmpty()) {
                        hasGroundedEvidence = true;
                    }
                    logAgentActivity(activityStore, profile, "info", "search",
                            "Found " + result.items().size() + " search results",
                            renderSearchActivity(result));
                }
                case FetchAction fetchAction -> {
                    BrowserPage page;
                    try {
                        page = browserTool.fetch(fetchAction.url());
                    } catch (IOException ex) {
                        logAgentActivity(activityStore, profile, "error", "fetch", "Browser fetch failed",
                                "Turn: " + turn + "\nURL: " + fetchAction.url() + "\nError: " + nonBlank(ex.getMessage()));
                        throw ex;
                    }
                    observations.add(new ToolObservation("fetch", fetchAction.url(), page.render()));
                    if (!page.content().isBlank()) {
                        hasGroundedEvidence = true;
                    }
                    logAgentActivity(activityStore, profile, "info", "fetch",
                            "Investigating " + fetchAction.url(), renderFetchActivity(page));
                }
                case ReadMemoryAction readMemoryAction -> observations.add(runMemoryRead(readMemoryAction));
                case WriteMemoryAction writeMemoryAction -> observations.add(runMemoryWrite(writeMemoryAction));
            }
        }

        logAgentActivity(activityStore, profile, "error", "social-media", "Social-media turn limit reached",
                "Search focus: " + searchFocus + "\nMax turns: " + config.maxTurns());
        return "Social-media did not complete within " + config.maxTurns() + " turns.";
    }

    public String collectQueuedSocialOpportunitiesBatch(String agentId, int count, int maxItems, String executionTarget) throws IOException, InterruptedException {
        List<SocialSearchQueueItem> dueItems = socialSearchQueueStore.dueItems(agentId, Math.max(1, maxItems));
        if (dueItems.isEmpty()) {
            return "";
        }
        int delaySeconds = queueDelaySeconds(agentId);
        StringBuilder out = new StringBuilder();
        int processed = 0;
        for (SocialSearchQueueItem item : dueItems) {
            if (processed > 0) {
                out.append("\n\n");
            }
            String result = collectSocialOpportunities(agentId, item.effectiveQuery(), count, executionTarget);
            int resultsSeen = extractSavedCount(result);
            SocialSearchQueueItem updated = item.afterRun(item.resultsSeen() + resultsSeen, delaySeconds);
            socialSearchQueueStore.markRun(agentId, updated);
            out.append("Queue item ").append(item.id()).append(":\n");
            out.append(result).append('\n');
            out.append("Advanced: ").append(item.summaryLine()).append(" -> pass ").append(updated.pass());
            processed++;
        }
        out.append("\n\nProcessed ").append(processed).append(" queued social search");
        if (processed != 1) {
            out.append("es");
        }
        out.append(".");
        return out.toString().trim();
    }

    public String addSocialQueueItem(String agentId, String site, String topic, String audience, String queryTemplate, String notes) throws IOException {
        return socialSearchQueueStore.add(agentId, site, topic, audience, queryTemplate, notes);
    }

    public String socialQueueReport(String agentId) throws IOException {
        return socialSearchQueueStore.renderList(agentId);
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

    private List<SocialOpportunityRecord> filterGroundedSocialOpportunities(List<SocialOpportunityRecord> records) {
        List<SocialOpportunityRecord> grounded = new ArrayList<>();
        for (SocialOpportunityRecord record : records) {
            if (record == null || record.sourceUrls() == null || record.sourceUrls().isEmpty()) {
                continue;
            }
            boolean hasUsableUrl = false;
            for (String url : record.sourceUrls()) {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    hasUsableUrl = true;
                    break;
                }
            }
            if (hasUsableUrl) {
                grounded.add(record);
            }
        }
        return grounded;
    }

    private String renderSocialOpportunitySummary(List<SocialOpportunityRecord> records) {
        StringBuilder out = new StringBuilder();
        out.append("Parsed social opportunities: ").append(records.size()).append('\n');
        for (SocialOpportunityRecord record : records) {
            out.append('\n').append("- ").append(record.title().isBlank() ? "(untitled)" : record.title());
            out.append(" | relevance: ").append(record.relevance().isBlank() ? "(unspecified)" : record.relevance());
            out.append(" | site: ").append(record.site().isBlank() ? "(unknown)" : record.site());
            out.append(" | community: ").append(record.community().isBlank() ? "(unknown)" : record.community());
            out.append(" | url: ").append(record.url().isBlank() ? "(none)" : record.url());
        }
        return out.toString().trim();
    }

    private LlmProvider autonomousProvider(String executionTarget, String preferredLocalPool) {
        String target = executionTarget == null ? "" : executionTarget.trim().toLowerCase();
        if ("local".equals(target)) {
            return workerPool.providerForPool(preferredLocalPool);
        }
        return manager;
    }

    private String preferredLocalPool(String agentId) throws IOException {
        return readAgentStatus(agentId).preferredLocalPool();
    }

    private AgentStatus readAgentStatus(String agentId) throws IOException {
        if (agentId == null || agentId.isBlank()) {
            return AgentStatus.parse("", config);
        }
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            return AgentStatus.parse("", config);
        }
        return AgentStatus.parse(readIfExists(profile.statusFile()), config);
    }

    private int queueDelaySeconds(String agentId) throws IOException {
        if (agentId == null || agentId.isBlank()) {
            return 60;
        }
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            return 60;
        }
        AgentStatus status = readAgentStatus(profile.id());
        return Math.max(5, status.runIntervalSeconds());
    }

    private LlmProvider coordinatorProvider(String agentId) throws IOException {
        AgentStatus status = readAgentStatus(agentId);
        return autonomousProvider(status.executionTarget(), status.preferredLocalPool());
    }

    public String usageReport() throws IOException {
        return usageTracker.renderText();
    }

    public String distillAgent(String agentId) throws IOException, InterruptedException {
        return withUsageAgent(agentId, () -> distillAgentInternal(agentId));
    }

    private String distillAgentInternal(String agentId) throws IOException, InterruptedException {
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

    private String withUsageAgent(String agentId, UsageAction action) throws IOException, InterruptedException {
        try {
            return UsageContext.withAgent(agentId, action::run);
        } catch (IOException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Unexpected usage-context failure.", ex);
        }
    }

    @FunctionalInterface
    private interface UsageAction {
        String run() throws IOException, InterruptedException;
    }

    private String readIfExists(java.nio.file.Path file) throws IOException {
        if (file == null || java.nio.file.Files.exists(file) == false) {
            return "";
        }
        return java.nio.file.Files.readString(file).trim();
    }

    private String loadPrimaryOutput(AgentProfile profile, AgentStatus status) throws IOException {
        String relative = status.primaryOutputFile();
        if (relative == null || relative.isBlank()) {
            return "";
        }
        String normalized = relative.replace('\\', '/');
        if (normalized.startsWith("workspace/")) {
            normalized = normalized.substring("workspace/".length());
        }
        return readIfExists(profile.workspaceDir().resolve(normalized));
    }

    private String groundedLightweightResponse(AgentRequest request, boolean styledOutput) throws IOException {
        String prompt = request.prompt() == null ? "" : request.prompt().trim();
        if (prompt.isBlank()) {
            return "";
        }
        if (isGreetingPrompt(prompt)) {
            return renderSimpleFinal(
                    "ready",
                    request.agentId() == null || request.agentId().isBlank() ? "Helion" : request.agentId() + " ready",
                    greetingSummary(request.agentId()),
                    greetingDetails(request.agentId()),
                    List.of("Ask for status, current output, or a specific task."),
                    List.of("Grounded from current agent status and output files."),
                    styledOutput);
        }
        if (looksLikeStatusPrompt(prompt)) {
            return renderSimpleFinal(
                    "ready",
                    statusTitle(request.agentId()),
                    statusSummary(request.agentId()),
                    statusDetails(request.agentId()),
                    List.of("Ask for a specific task if you want the agent to do work next."),
                    List.of("Grounded from the configured primary output file and current agent status."),
                    styledOutput);
        }
        return "";
    }

    private boolean isGreetingPrompt(String prompt) {
        String normalized = prompt.trim().toLowerCase();
        return normalized.equals("hi")
                || normalized.equals("hello")
                || normalized.equals("hey")
                || normalized.equals("yo")
                || normalized.equals("good morning")
                || normalized.equals("good afternoon")
                || normalized.equals("good evening");
    }

    private boolean looksLikeStatusPrompt(String prompt) {
        String normalized = prompt.trim().toLowerCase();
        return normalized.contains("status")
                || normalized.contains("what are you working on")
                || normalized.contains("what are you doing")
                || normalized.contains("pipeline")
                || normalized.contains("current output")
                || normalized.contains("how many prospects")
                || normalized.contains("show prospects")
                || normalized.contains("what have you found");
    }

    private String greetingSummary(String agentId) throws IOException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            return "Ready to help.";
        }
        AgentStatus status = AgentStatus.parse(readIfExists(profile.statusFile()), config);
        String outputSummary = summarizePrimaryOutput(profile, status);
        return profile.id() + " is " + status.runState() + " on " + status.executionTarget() + ". " + outputSummary;
    }

    private String greetingDetails(String agentId) throws IOException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            return "Ask for a business task, status summary, or a specific workflow.";
        }
        AgentStatus status = AgentStatus.parse(readIfExists(profile.statusFile()), config);
        return """
                Agent: %s
                Run state: %s
                Execution target: %s
                Primary output: %s
                """.formatted(
                profile.id(),
                status.runState(),
                status.executionTarget(),
                status.primaryOutputFile().isBlank() ? "(none)" : status.primaryOutputFile()).trim();
    }

    private String statusTitle(String agentId) {
        return (agentId == null || agentId.isBlank() ? "Agent" : agentId) + " status";
    }

    private String statusSummary(String agentId) throws IOException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            return "No specific agent selected.";
        }
        AgentStatus status = AgentStatus.parse(readIfExists(profile.statusFile()), config);
        return summarizePrimaryOutput(profile, status);
    }

    private String statusDetails(String agentId) throws IOException {
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            return "No specific agent selected.";
        }
        AgentStatus status = AgentStatus.parse(readIfExists(profile.statusFile()), config);
        String outputContent = loadPrimaryOutput(profile, status);
        StringBuilder out = new StringBuilder();
        out.append("Run state: ").append(status.runState()).append('\n');
        out.append("Execution target: ").append(status.executionTarget()).append('\n');
        out.append("Primary output file: ").append(status.primaryOutputFile().isBlank() ? "(none)" : status.primaryOutputFile()).append('\n');
        out.append('\n');
        out.append("Grounded output summary:\n").append(summarizePrimaryOutput(profile, status)).append('\n');
        out.append('\n');
        out.append("Current output preview:\n").append(outputContent.isBlank() ? "(empty)" : TextUtils.limit(outputContent, 1200));
        return out.toString().trim();
    }

    private String summarizePrimaryOutput(AgentProfile profile, AgentStatus status) throws IOException {
        if (status.primaryOutputFile().isBlank()) {
            return "No primary output file is configured.";
        }
        Path outputFile = AgentOutputResolver.resolvePrimaryOutputFile(profile, config, status.primaryOutputFile());
        if (!Files.exists(outputFile)) {
            return "The primary output file does not exist yet.";
        }
        if ("prospecting".equals(profile.id())) {
            Path csvFile = profile.workspaceDir().resolve("prospects.csv");
            int count = countCsvRecords(csvFile);
            return count == 0
                    ? "There are currently 0 saved prospects in the tracked prospect list."
                    : "There are currently " + count + " saved prospects in the tracked prospect list.";
        }
        String content = Files.readString(outputFile).trim();
        if (content.isBlank()) {
            return "The primary output file exists but is currently empty.";
        }
        return "The primary output file has content and is being used as the active deliverable.";
    }

    private int countCsvRecords(Path csvFile) throws IOException {
        if (csvFile == null || !Files.exists(csvFile)) {
            return 0;
        }
        List<String> lines = Files.readAllLines(csvFile);
        int count = 0;
        for (int i = 1; i < lines.size(); i++) {
            if (!lines.get(i).trim().isBlank()) {
                count++;
            }
        }
        return count;
    }

    private String renderSimpleFinal(String status, String title, String summary, String details, List<String> nextSteps, List<String> sources, boolean styledOutput) {
        FinalResponse response = new FinalResponse(status, title, summary, details, nextSteps, sources);
        return styledOutput ? response.render() : response.renderPlain();
    }

    private void logAgentActivity(AgentActivityStore store, AgentProfile profile, String level, String task, String summary, String details) {
        if (profile == null) {
            return;
        }
        try {
            store.append(profile, new AgentActivityEntry(java.time.LocalDateTime.now(), level, task, summary, details));
        } catch (IOException ignored) {
        }
    }

    private String prospectingActionDetails(ManagerAction action) {
        return switch (action) {
            case WorkerAction workerAction -> "Worker title: " + workerAction.title() + "\nPrompt:\n" + TextUtils.limit(workerAction.prompt(), 1200);
            case SearchAction searchAction -> "Query: " + searchAction.query() + "\nLimit: " + searchAction.limit();
            case FetchAction fetchAction -> "URL: " + fetchAction.url();
            case ReadMemoryAction readMemoryAction -> "Key: " + readMemoryAction.key();
            case WriteMemoryAction writeMemoryAction -> "Key: " + writeMemoryAction.key() + "\nContent:\n" + TextUtils.limit(writeMemoryAction.content(), 1200);
            case FinalAction finalAction -> "Final content preview:\n" + TextUtils.limit(finalAction.content(), 1200);
        };
    }

    private String actionName(ManagerAction action) {
        return switch (action) {
            case WorkerAction ignored -> "WORKER";
            case SearchAction ignored -> "SEARCH";
            case FetchAction ignored -> "FETCH";
            case ReadMemoryAction ignored -> "READ_MEMORY";
            case WriteMemoryAction ignored -> "WRITE_MEMORY";
            case FinalAction ignored -> "FINAL";
        };
    }

    private String renderSearchActivity(BrowserSearchResult result) {
        StringBuilder out = new StringBuilder();
        out.append("Query: ").append(result.query()).append('\n');
        out.append("Results found: ").append(result.items().size()).append('\n');
        if (result.note() != null && !result.note().isBlank()) {
            out.append("Note: ").append(result.note()).append('\n');
        }
        for (int i = 0; i < result.items().size(); i++) {
            SearchResultItem item = result.items().get(i);
            String relevance = relevanceLabel(result.query(), item.title(), item.url(), item.snippet());
            String companyGuess = companyGuess(item.title(), item.url());
            out.append('\n').append(i + 1).append(". ").append(item.title()).append('\n');
            out.append("URL: ").append(item.url()).append('\n');
            out.append("Domain: ").append(domainOf(item.url())).append('\n');
            out.append("Company guess: ").append(companyGuess).append('\n');
            out.append("Relevance: ").append(relevance).append('\n');
            String keywordSignals = keywordSignals(result.query(), item.title(), item.url(), item.snippet());
            if (!keywordSignals.isBlank()) {
                out.append("Matched signals: ").append(keywordSignals).append('\n');
            }
            if (!item.snippet().isBlank()) {
                out.append("Snippet: ").append(item.snippet()).append('\n');
            }
        }
        return out.toString().trim();
    }

    private String renderFetchActivity(BrowserPage page) {
        StringBuilder out = new StringBuilder();
        out.append("URL: ").append(page.url()).append('\n');
        out.append("Domain: ").append(domainOf(page.url())).append('\n');
        if (page.title() != null && !page.title().isBlank()) {
            out.append("Title: ").append(page.title()).append('\n');
        }
        out.append("Company guess: ").append(companyGuess(page.title(), page.url())).append('\n');
        out.append("Page type: ").append(classifyPageType(page.url(), page.title(), page.content())).append('\n');
        String contactSignals = contactSignals(page.content());
        if (!contactSignals.isBlank()) {
            out.append("Contact signals: ").append(contactSignals).append('\n');
        }
        out.append("Tube-work relevance: ").append(relevanceLabel("", page.title(), page.url(), page.content())).append('\n');
        String contentSignals = contentSignals(page.content());
        if (!contentSignals.isBlank()) {
            out.append("Content signals: ").append(contentSignals).append('\n');
        }
        if (page.note() != null && !page.note().isBlank()) {
            out.append("Note: ").append(page.note()).append('\n');
        }
        out.append('\n').append("Preview:\n").append(TextUtils.limit(page.content(), 1200));
        return out.toString().trim();
    }

    private String renderProspectSummary(List<ProspectRecord> records) {
        StringBuilder out = new StringBuilder();
        out.append("Parsed prospects: ").append(records.size()).append('\n');
        for (ProspectRecord record : records) {
            out.append('\n').append("- ").append(record.company().isBlank() ? "(unknown company)" : record.company());
            out.append(" | fit: ").append(record.fitScore().isBlank() ? "(unspecified)" : record.fitScore());
            out.append(" | website: ").append(record.website().isBlank() ? "(none)" : record.website());
            out.append(" | location: ").append(record.location().isBlank() ? "(unknown)" : record.location());
            out.append(" | contact: ").append(contactSummary(record));
            out.append(" | sources: ").append(record.sourceUrls() == null ? 0 : record.sourceUrls().size());
        }
        return out.toString().trim();
    }

    private String relevanceLabel(String query, String title, String url, String text) {
        int score = relevanceScore(query, title, url, text);
        if (score >= 5) {
            return "high";
        }
        if (score >= 3) {
            return "medium";
        }
        return "low";
    }

    private int relevanceScore(String query, String title, String url, String text) {
        String combined = ((title == null ? "" : title) + " " + (url == null ? "" : url) + " " + (text == null ? "" : text)).toLowerCase();
        int score = 0;
        for (String token : significantTokens(query)) {
            if (combined.contains(token)) {
                score++;
            }
        }
        String[] businessSignals = {"fabrication", "fabricator", "tube", "chassis", "roll cage", "off-road", "motorsport", "welding", "cage", "frame"};
        for (String signal : businessSignals) {
            if (combined.contains(signal)) {
                score++;
            }
        }
        return score;
    }

    private String keywordSignals(String query, String title, String url, String text) {
        String combined = ((title == null ? "" : title) + " " + (url == null ? "" : url) + " " + (text == null ? "" : text)).toLowerCase();
        List<String> hits = new ArrayList<>();
        for (String token : significantTokens(query)) {
            if (combined.contains(token) && !hits.contains(token)) {
                hits.add(token);
            }
        }
        String[] businessSignals = {"fabrication", "tube", "chassis", "roll cage", "off-road", "motorsport", "welding", "cage", "frame", "contact"};
        for (String signal : businessSignals) {
            if (combined.contains(signal) && !hits.contains(signal)) {
                hits.add(signal);
            }
        }
        return hits.isEmpty() ? "" : String.join(", ", hits);
    }

    private List<String> significantTokens(String query) {
        List<String> tokens = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return tokens;
        }
        String lower = query.toLowerCase();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                current.append(c);
            } else if (!current.isEmpty()) {
                addToken(tokens, current.toString());
                current.setLength(0);
            }
        }
        if (!current.isEmpty()) {
            addToken(tokens, current.toString());
        }
        return tokens;
    }

    private void addToken(List<String> tokens, String token) {
        if (token.length() < 3) {
            return;
        }
        if (token.equals("the") || token.equals("and") || token.equals("for") || token.equals("with")) {
            return;
        }
        if (!tokens.contains(token)) {
            tokens.add(token);
        }
    }

    private String companyGuess(String title, String url) {
        String candidate = title == null ? "" : title.trim();
        if (!candidate.isBlank()) {
            int cut = candidate.indexOf('|');
            if (cut < 0) {
                cut = candidate.indexOf('-');
            }
            if (cut > 0) {
                candidate = candidate.substring(0, cut).trim();
            }
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        String domain = domainOf(url);
        if (domain.isBlank()) {
            return "(unknown)";
        }
        int cut = domain.indexOf('.');
        String root = cut > 0 ? domain.substring(0, cut) : domain;
        return root.replace('-', ' ');
    }

    private String domainOf(String url) {
        if (url == null || url.isBlank()) {
            return "(unknown)";
        }
        try {
            String host = URI.create(url.trim()).getHost();
            if (host == null || host.isBlank()) {
                return "(unknown)";
            }
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception ex) {
            return "(unknown)";
        }
    }

    private String classifyPageType(String url, String title, String content) {
        String combined = ((url == null ? "" : url) + " " + (title == null ? "" : title) + " " + (content == null ? "" : TextUtils.limit(content, 400))).toLowerCase();
        if (combined.contains("contact")) {
            return "contact";
        }
        if (combined.contains("portfolio") || combined.contains("gallery") || combined.contains("projects")) {
            return "portfolio";
        }
        if (combined.contains("about")) {
            return "about";
        }
        if (combined.contains("services")) {
            return "services";
        }
        return "general";
    }

    private String contactSignals(String content) {
        List<String> parts = new ArrayList<>();
        List<String> emails = extractEmails(content, 3);
        List<String> phones = extractPhones(content, 2);
        if (!emails.isEmpty()) {
            parts.add("emails=" + String.join(", ", emails));
        }
        if (!phones.isEmpty()) {
            parts.add("phones=" + String.join(", ", phones));
        }
        return String.join(" | ", parts);
    }

    private String contentSignals(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String lower = content.toLowerCase();
        List<String> hits = new ArrayList<>();
        String[] signals = {"tube", "chassis", "roll cage", "fabrication", "welding", "cnc", "plasma", "jig", "production", "prototype", "off-road", "motorsport"};
        for (String signal : signals) {
            if (lower.contains(signal)) {
                hits.add(signal);
            }
        }
        return hits.isEmpty() ? "" : String.join(", ", hits);
    }

    private List<String> extractEmails(String content, int limit) {
        List<String> emails = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return emails;
        }
        String text = content;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '@') {
                continue;
            }
            int start = i - 1;
            while (start >= 0 && isEmailChar(text.charAt(start))) {
                start--;
            }
            int end = i + 1;
            while (end < text.length() && isEmailChar(text.charAt(end))) {
                end++;
            }
            String candidate = text.substring(start + 1, end).trim();
            if (candidate.contains(".") && !emails.contains(candidate)) {
                emails.add(candidate);
                if (emails.size() >= limit) {
                    break;
                }
            }
        }
        return emails;
    }

    private boolean isEmailChar(char c) {
        return Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-' || c == '@' || c == '+';
    }

    private List<String> extractPhones(String content, int limit) {
        List<String> phones = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return phones;
        }
        String text = content;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c) || c == '+' || c == '-' || c == ' ' || c == '(' || c == ')') {
                current.append(c);
            } else {
                maybeAddPhone(phones, current.toString(), limit);
                current.setLength(0);
                if (phones.size() >= limit) {
                    break;
                }
            }
        }
        maybeAddPhone(phones, current.toString(), limit);
        return phones;
    }

    private void maybeAddPhone(List<String> phones, String raw, int limit) {
        if (phones.size() >= limit) {
            return;
        }
        String candidate = raw.trim();
        if (candidate.length() < 10) {
            return;
        }
        int digits = 0;
        for (int i = 0; i < candidate.length(); i++) {
            if (Character.isDigit(candidate.charAt(i))) {
                digits++;
            }
        }
        if (digits < 10) {
            return;
        }
        if (!phones.contains(candidate)) {
            phones.add(candidate);
        }
    }

    private String contactSummary(ProspectRecord record) {
        List<String> parts = new ArrayList<>();
        if (record.contactName() != null && !record.contactName().isBlank()) {
            parts.add(record.contactName());
        }
        if (record.contactRole() != null && !record.contactRole().isBlank()) {
            parts.add(record.contactRole());
        }
        if (record.contactEmail() != null && !record.contactEmail().isBlank()) {
            parts.add(record.contactEmail());
        } else if (record.phone() != null && !record.phone().isBlank()) {
            parts.add(record.phone());
        }
        return parts.isEmpty() ? "(none)" : String.join(" / ", parts);
    }
}
