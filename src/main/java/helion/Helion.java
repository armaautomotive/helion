package helion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.awt.Desktop;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Helion {
    private static final String SESSION_FLAG = "--session";
    private static final String WEB_FLAG = "--web";
    private static final String WEB_OPEN_FLAG = "--web-open";
    private static final String AGENT_FLAG = "--agent";

    private Helion() {
    }

    public static void main(String[] args) {
        try {
            HelionConfig config = HelionConfig.load();
            AgentRegistry agentRegistry = ProviderFactory.createAgentRegistry(config);
            CompanyDataSources companyDataSources = ProviderFactory.createCompanyDataSources(config);
            BusinessAgent agent = ProviderFactory.createAgent(config);
            startSupervisor(config, agent, agentRegistry);
            CliOptions options = parseArgs(args);
            if (options.webMode()) {
                runWeb(config, agent, agentRegistry, companyDataSources, options.openWeb());
                return;
            }
            if (options.sessionMode()) {
                runSession(config, agent, agentRegistry, companyDataSources, options);
                return;
            }

            AgentMode mode = AgentMode.fromCliArg(options.modeArg());
            String prompt = extractPrompt(options.remainingArgs(), options.modeArg());
            if (prompt.isBlank()) {
                printUsage();
                return;
            }

            String response = agent.respond(new AgentRequest(mode, prompt, options.agentId()));
            System.out.println(response);
        } catch (Exception ex) {
            System.err.println(Ansi.red("helion error: " + ex.getMessage()));
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static String extractPrompt(String[] args, String modeArg) throws IOException {
        int promptStart = AgentMode.looksLikeMode(modeArg) ? 1 : 0;
        if (args.length > promptStart) {
            return String.join(" ", java.util.Arrays.copyOfRange(args, promptStart, args.length)).trim();
        }
        return readStdIn().trim();
    }

    private static void runWeb(HelionConfig config, BusinessAgent agent, AgentRegistry agentRegistry, CompanyDataSources companyDataSources, boolean openBrowser) throws IOException {
        HelionWebServer server = new HelionWebServer(config, agent, agentRegistry, companyDataSources);
        server.start();
        System.out.println(Ansi.bold(Ansi.blue("Helion web UI started.")));
        System.out.println(Ansi.blue("Open: ") + server.baseUrl());
        if (openBrowser) {
            openBrowser(server.baseUrl());
        }
        System.out.println(Ansi.blue("Press Ctrl+C to stop."));
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static void runSession(HelionConfig config, BusinessAgent agent, AgentRegistry agentRegistry, CompanyDataSources companyDataSources, CliOptions options) throws IOException {
        AgentMode currentMode = AgentMode.fromCliArg(options.modeArg());
        String currentAgent = options.agentId();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        HelionWebServer webServer = null;

        printSessionHeader(currentMode, currentAgent);
        while (true) {
            String promptAgent = currentAgent == null || currentAgent.isBlank() ? "default" : currentAgent;
            System.out.print(Ansi.cyan("helion[" + promptAgent + ":" + currentMode.name().toLowerCase() + "]> "));
            String line = reader.readLine();
            if (line == null) {
                System.out.println();
                break;
            }

            String input = line.trim();
            if (input.isEmpty()) {
                continue;
            }
            if ("/exit".equalsIgnoreCase(input) || "/quit".equalsIgnoreCase(input)) {
                break;
            }
            if ("/help".equalsIgnoreCase(input)) {
                printSessionHelp();
                continue;
            }
            if ("/distill".equalsIgnoreCase(input)) {
                runDistill(agent, currentAgent);
                continue;
            }
            if ("/prospect".equalsIgnoreCase(input)) {
                runQueuedProspect(agentRegistry, agent, currentAgent);
                continue;
            }
            if (input.toLowerCase().startsWith("/prospect ")) {
                runProspect(agentRegistry, agent, currentAgent, input.substring("/prospect ".length()).trim());
                continue;
            }
            if ("/queue".equalsIgnoreCase(input) || "/queue list".equalsIgnoreCase(input)) {
                printProspectQueue(agentRegistry, agent, currentAgent);
                continue;
            }
            if (input.toLowerCase().startsWith("/queue add ")) {
                addProspectQueueItem(agentRegistry, agent, currentAgent, input.substring("/queue add ".length()).trim());
                continue;
            }
            if (input.toLowerCase().startsWith("/distill ")) {
                runDistill(agent, input.substring("/distill ".length()).trim());
                continue;
            }
            if ("/usage".equalsIgnoreCase(input)) {
                printUsageStats(agent);
                continue;
            }
            if ("/emailconfig".equalsIgnoreCase(input)) {
                printEmailConfig(agent);
                continue;
            }
            if (input.toLowerCase().startsWith("/draftemail ")) {
                saveDraftEmail(agentRegistry, agent, currentAgent, input.substring("/draftemail ".length()).trim());
                continue;
            }
            if ("/openweb".equalsIgnoreCase(input) || "/web".equalsIgnoreCase(input)) {
                webServer = ensureWebServerStarted(config, agent, agentRegistry, companyDataSources, webServer);
                openBrowser(webServer.baseUrl());
                continue;
            }
            if ("/company".equalsIgnoreCase(input) || "/company list".equalsIgnoreCase(input)) {
                printCompanyDataSources(companyDataSources);
                continue;
            }
            if (input.toLowerCase().startsWith("/company add ")) {
                String pathValue = input.substring("/company add ".length()).trim();
                addCompanyDataSource(companyDataSources, pathValue);
                continue;
            }
            if (input.toLowerCase().startsWith("/company edit ")) {
                editCompanyDataSource(companyDataSources, input.substring("/company edit ".length()).trim());
                continue;
            }
            if (input.toLowerCase().startsWith("/company delete ")) {
                deleteCompanyDataSource(companyDataSources, input.substring("/company delete ".length()).trim());
                continue;
            }
            if ("/agents".equalsIgnoreCase(input)) {
                printAgents(agentRegistry);
                continue;
            }
            if ("/agent".equalsIgnoreCase(input)) {
                System.out.println("Current agent: " + (currentAgent == null || currentAgent.isBlank() ? "default" : currentAgent));
                continue;
            }
            if (input.toLowerCase().startsWith("/agent ")) {
                String agentId = input.substring("/agent ".length()).trim();
                if (agentRegistry.load(agentId) == null) {
                    System.out.println(Ansi.yellow("Unknown agent: " + agentId));
                    continue;
                }
                currentAgent = agentId;
                System.out.println(Ansi.green("Agent set to " + currentAgent));
                continue;
            }
            if ("/mode".equalsIgnoreCase(input)) {
                System.out.println(Ansi.blue("Current mode: ") + currentMode.name().toLowerCase());
                continue;
            }
            if ("/status".equalsIgnoreCase(input)) {
                printAgentStatus(config, agentRegistry, currentAgent, agent);
                continue;
            }
            if (input.toLowerCase().startsWith("/mode ")) {
                String modeValue = input.substring("/mode ".length()).trim();
                if (!AgentMode.looksLikeMode(modeValue)) {
                    System.out.println(Ansi.yellow("Unknown mode: " + modeValue));
                    continue;
                }
                currentMode = AgentMode.fromCliArg(modeValue);
                System.out.println(Ansi.green("Mode set to " + currentMode.name().toLowerCase()));
                continue;
            }

            try {
                String response = agent.respond(new AgentRequest(currentMode, input, currentAgent));
                System.out.println();
                System.out.println(response);
                System.out.println();
            } catch (Exception ex) {
                System.out.println();
                System.out.println(Ansi.red("helion error: " + ex.getMessage()));
                System.out.println();
            }
        }
    }

    private static void printSessionHeader(AgentMode mode, String agentId) {
        System.out.println(Ansi.bold(Ansi.blue("Helion session started.")));
        System.out.println(Ansi.blue("Agent: ") + (agentId == null || agentId.isBlank() ? "default" : agentId));
        System.out.println(Ansi.blue("Mode: ") + mode.name().toLowerCase());
        System.out.println(Ansi.blue("Commands: ") + "/help, /agents, /agent <id>, /status, /mode <plan|analyze|email|general>, /quit");
        System.out.println();
    }

    private static void printSessionHelp() {
        System.out.println(Ansi.bold("Session commands:"));
        System.out.println("/help  Show commands");
        System.out.println("/distill  Refresh distilled files for the current agent");
        System.out.println("/distill <agent-id>  Refresh distilled files for a specific agent");
        System.out.println("/prospect  Run the next due queued prospect search for the current agent");
        System.out.println("/prospect <search focus>  Run an ad-hoc prospect search and save the results");
        System.out.println("/queue  List queued prospect searches for the current agent");
        System.out.println("/queue add <query template> | <region> | <city> | <industry> | <notes>  Add a queued prospect search");
        System.out.println("/usage  Show per-model usage stats");
        System.out.println("/emailconfig  Show configured email transport settings");
        System.out.println("/draftemail <to> | <subject> | <body>  Save a draft email in the current agent workspace");
        System.out.println("/openweb  Start the local web UI if needed and open it in the default browser");
        System.out.println("/company  List company data directories");
        System.out.println("/company add <path>  Add a company data directory");
        System.out.println("/company edit <index> <path>  Replace a company data directory");
        System.out.println("/company delete <index>  Remove a company data directory");
        System.out.println("/agents  List available agents");
        System.out.println("/agent  Show current agent");
        System.out.println("/agent <id>  Switch agent");
        System.out.println("/status  Show current agent status");
        System.out.println("/mode  Show current mode");
        System.out.println("/mode <plan|analyze|email|general>  Switch modes");
        System.out.println("/quit  Exit session");
        System.out.println();
    }

    private static void printAgents(AgentRegistry agentRegistry) throws IOException {
        List<String> agentIds = agentRegistry.listAgentIds();
        if (agentIds.isEmpty()) {
            System.out.println(Ansi.yellow("No agents found."));
            return;
        }
        System.out.println(Ansi.bold("Agents:"));
        for (String agentId : agentIds) {
            System.out.println("- " + agentId);
        }
    }

    private static void printAgentStatus(HelionConfig config, AgentRegistry agentRegistry, String currentAgent, BusinessAgent agent) throws IOException {
        AgentProfile profile = agentRegistry.load(currentAgent);
        if (profile == null) {
            System.out.println(Ansi.yellow("No specific agent selected."));
            return;
        }
        String status = "";
        if (java.nio.file.Files.exists(profile.statusFile())) {
            status = java.nio.file.Files.readString(profile.statusFile()).trim();
        }
        AgentStatus parsedStatus = AgentStatus.parse(status, config);
        AgentRuntime runtime = new AgentRuntimeStore().read(profile, parsedStatus.executionTarget());
        System.out.println(Ansi.bold("Agent: ") + profile.id());
        System.out.println(parsedStatus.renderedStatus());
        System.out.println();
        System.out.println(Ansi.bold("Runtime:"));
        System.out.println(runtime.render());
        System.out.println();
        System.out.println(Ansi.bold("Model usage:"));
        System.out.println(agent.usageReport());
    }

    private static void printCompanyDataSources(CompanyDataSources companyDataSources) throws IOException {
        System.out.println(Ansi.bold("Company data directories:"));
        System.out.println(companyDataSources.renderList());
    }

    private static void printUsageStats(BusinessAgent agent) throws IOException {
        System.out.println(Ansi.bold("Model usage:"));
        System.out.println(agent.usageReport());
    }

    private static void printEmailConfig(BusinessAgent agent) {
        System.out.println(Ansi.bold("Email configuration:"));
        System.out.println(agent.emailConfigReport());
    }

    private static void runDistill(BusinessAgent agent, String agentId) {
        if (agentId == null || agentId.isBlank()) {
            System.out.println(Ansi.yellow("No agent selected for distillation."));
            return;
        }
        try {
            System.out.println(Ansi.green(agent.distillAgent(agentId)));
        } catch (Exception ex) {
            System.out.println(Ansi.red("Distillation failed: " + ex.getMessage()));
        }
    }

    private static void runProspect(AgentRegistry agentRegistry, BusinessAgent agent, String currentAgent, String value) {
        if (currentAgent == null || currentAgent.isBlank()) {
            System.out.println(Ansi.yellow("Select an agent first with /agent <id>."));
            return;
        }
        if (agentRegistry.load(currentAgent) == null) {
            System.out.println(Ansi.yellow("Unknown agent: " + currentAgent));
            return;
        }
        if (value.isBlank()) {
            System.out.println(Ansi.yellow("Usage: /prospect <search focus>"));
            return;
        }
        try {
            System.out.println(Ansi.green(agent.collectProspects(currentAgent, value, 5)));
        } catch (Exception ex) {
            System.out.println(Ansi.red("Prospecting failed: " + ex.getMessage()));
        }
    }

    private static void runQueuedProspect(AgentRegistry agentRegistry, BusinessAgent agent, String currentAgent) {
        if (currentAgent == null || currentAgent.isBlank()) {
            System.out.println(Ansi.yellow("Select an agent first with /agent <id>."));
            return;
        }
        if (agentRegistry.load(currentAgent) == null) {
            System.out.println(Ansi.yellow("Unknown agent: " + currentAgent));
            return;
        }
        try {
            System.out.println(Ansi.green(agent.collectQueuedProspects(currentAgent, 5)));
        } catch (Exception ex) {
            System.out.println(Ansi.red("Queued prospecting failed: " + ex.getMessage()));
        }
    }

    private static void printProspectQueue(AgentRegistry agentRegistry, BusinessAgent agent, String currentAgent) {
        if (currentAgent == null || currentAgent.isBlank()) {
            System.out.println(Ansi.yellow("Select an agent first with /agent <id>."));
            return;
        }
        if (agentRegistry.load(currentAgent) == null) {
            System.out.println(Ansi.yellow("Unknown agent: " + currentAgent));
            return;
        }
        try {
            System.out.println(Ansi.bold("Prospect queue:"));
            System.out.println(agent.prospectQueueReport(currentAgent));
        } catch (Exception ex) {
            System.out.println(Ansi.red("Queue read failed: " + ex.getMessage()));
        }
    }

    private static void addProspectQueueItem(AgentRegistry agentRegistry, BusinessAgent agent, String currentAgent, String value) {
        if (currentAgent == null || currentAgent.isBlank()) {
            System.out.println(Ansi.yellow("Select an agent first with /agent <id>."));
            return;
        }
        if (agentRegistry.load(currentAgent) == null) {
            System.out.println(Ansi.yellow("Unknown agent: " + currentAgent));
            return;
        }
        String[] parts = value.split("\\|", 5);
        if (parts.length < 4) {
            System.out.println(Ansi.yellow("Usage: /queue add <query template> | <region> | <city> | <industry> | <notes>"));
            return;
        }
        String queryTemplate = parts[0].trim();
        String region = parts[1].trim();
        String city = parts[2].trim();
        String industry = parts[3].trim();
        String notes = parts.length >= 5 ? parts[4].trim() : "";
        try {
            System.out.println(Ansi.green(agent.addProspectQueueItem(currentAgent, queryTemplate, region, city, industry, notes)));
        } catch (Exception ex) {
            System.out.println(Ansi.red("Queue add failed: " + ex.getMessage()));
        }
    }

    private static void saveDraftEmail(AgentRegistry agentRegistry, BusinessAgent agent, String currentAgent, String value) throws IOException {
        if (currentAgent == null || currentAgent.isBlank()) {
            System.out.println(Ansi.yellow("Select an agent first with /agent <id>."));
            return;
        }
        if (agentRegistry.load(currentAgent) == null) {
            System.out.println(Ansi.yellow("Unknown agent: " + currentAgent));
            return;
        }
        String[] parts = value.split("\\|", 3);
        if (parts.length < 3) {
            System.out.println(Ansi.yellow("Usage: /draftemail <to> | <subject> | <body>"));
            return;
        }
        String to = parts[0].trim();
        String subject = parts[1].trim();
        String body = parts[2].trim();
        String result = agent.saveEmailDraft(currentAgent, to, subject, body, "Saved from session command.");
        System.out.println(Ansi.green(result));
    }

    private static void addCompanyDataSource(CompanyDataSources companyDataSources, String pathValue) throws IOException {
        if (pathValue.isBlank()) {
            System.out.println(Ansi.yellow("Usage: /company add <path>"));
            return;
        }
        companyDataSources.add(java.nio.file.Path.of(pathValue));
        System.out.println(Ansi.green("Added company data directory."));
    }

    private static void editCompanyDataSource(CompanyDataSources companyDataSources, String value) throws IOException {
        String[] parts = value.split("\\s+", 2);
        if (parts.length < 2) {
            System.out.println(Ansi.yellow("Usage: /company edit <index> <path>"));
            return;
        }
        int index = parseOneBasedIndex(parts[0]);
        companyDataSources.update(index, java.nio.file.Path.of(parts[1].trim()));
        System.out.println(Ansi.green("Updated company data directory."));
    }

    private static void deleteCompanyDataSource(CompanyDataSources companyDataSources, String value) throws IOException {
        if (value.isBlank()) {
            System.out.println(Ansi.yellow("Usage: /company delete <index>"));
            return;
        }
        int index = parseOneBasedIndex(value.trim());
        companyDataSources.remove(index);
        System.out.println(Ansi.green("Removed company data directory."));
    }

    private static int parseOneBasedIndex(String value) {
        try {
            return Integer.parseInt(value.trim()) - 1;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid index: " + value);
        }
    }

    private static String readStdIn() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
        return builder.toString();
    }

    private static void printUsage() {
        System.out.println("Usage: helion [--agent <id>] [plan|analyze|email|general] <prompt>");
        System.out.println("Usage: helion --session [--agent <id>] [plan|analyze|email|general]");
        System.out.println("Usage: helion --web");
        System.out.println("Usage: helion --web-open");
        System.out.println("Example: helion plan Launch a niche B2B software consultancy");
        System.out.println("Example: helion --agent prospecting general Find likely buyers for our CNC tube notcher");
        System.out.println("Example: helion --session --agent prospecting analyze");
        System.out.println("Example: helion --web");
        System.out.println("Example: helion --web-open");
        System.out.println("Config via env: HELION_MANAGER_PROVIDER, HELION_WORKER_PROVIDER, HELION_ENABLE_BROWSER, HELION_ENABLE_MEMORY, HELION_LLAMACPP_URL");
    }

    private static void startSupervisor(HelionConfig config, BusinessAgent agent, AgentRegistry agentRegistry) {
        AgentSupervisor supervisor = new AgentSupervisor(config, agent, agentRegistry, "");
        Thread thread = new Thread(() -> {
            try {
                supervisor.runLoop();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                System.err.println(Ansi.red("helion supervisor error: " + ex.getMessage()));
            }
        }, "helion-supervisor");
        thread.setDaemon(true);
        thread.start();
    }

    private static CliOptions parseArgs(String[] args) {
        boolean sessionMode = false;
        boolean webMode = false;
        boolean openWeb = false;
        String agentId = "";
        List<String> remaining = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (SESSION_FLAG.equalsIgnoreCase(arg)) {
                sessionMode = true;
                continue;
            }
            if (WEB_FLAG.equalsIgnoreCase(arg)) {
                webMode = true;
                continue;
            }
            if (WEB_OPEN_FLAG.equalsIgnoreCase(arg)) {
                webMode = true;
                openWeb = true;
                continue;
            }
            if (AGENT_FLAG.equalsIgnoreCase(arg)) {
                if (i + 1 < args.length) {
                    agentId = args[++i].trim();
                }
                continue;
            }
            remaining.add(arg);
        }

        String modeArg = remaining.isEmpty() ? "" : remaining.get(0);
        return new CliOptions(sessionMode, webMode, openWeb, agentId, modeArg, remaining.toArray(new String[0]));
    }

    private record CliOptions(boolean sessionMode, boolean webMode, boolean openWeb, String agentId, String modeArg, String[] remainingArgs) {
    }

    private static HelionWebServer ensureWebServerStarted(HelionConfig config, BusinessAgent agent, AgentRegistry agentRegistry, CompanyDataSources companyDataSources, HelionWebServer current) throws IOException {
        if (current != null) {
            System.out.println(Ansi.green("Helion web UI already running at " + current.baseUrl()));
            return current;
        }
        HelionWebServer server = new HelionWebServer(config, agent, agentRegistry, companyDataSources);
        server.start();
        System.out.println(Ansi.green("Helion web UI started at " + server.baseUrl()));
        return server;
    }

    private static void openBrowser(String url) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                System.out.println(Ansi.yellow("Desktop browser launch is not supported. Open this URL manually: " + url));
                return;
            }
            Desktop.getDesktop().browse(URI.create(url));
            System.out.println(Ansi.green("Opened browser: " + url));
        } catch (Exception ex) {
            System.out.println(Ansi.yellow("Could not open browser automatically. Open this URL manually: " + url));
        }
    }
}
