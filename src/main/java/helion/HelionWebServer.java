package helion;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HelionWebServer {
    private final HelionConfig config;
    private final BusinessAgent agent;
    private final AgentRegistry agentRegistry;
    private final CompanyDataSources companyDataSources;
    private final HttpServer server;

    public HelionWebServer(HelionConfig config, BusinessAgent agent, AgentRegistry agentRegistry, CompanyDataSources companyDataSources) throws IOException {
        this.config = config;
        this.agent = agent;
        this.agentRegistry = agentRegistry;
        this.companyDataSources = companyDataSources;
        this.server = HttpServer.create(new InetSocketAddress(config.webHost(), config.webPort()), 0);
        registerRoutes();
    }

    public void start() {
        server.start();
    }

    public String baseUrl() {
        return "http://" + config.webHost() + ":" + config.webPort();
    }

    private void registerRoutes() {
        server.createContext("/", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            sendHtml(exchange, HelionWebUi.indexHtml());
        });

        server.createContext("/api/agents", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            StringBuilder json = new StringBuilder();
            json.append("{\"agents\":[");
            List<String> ids = agentRegistry.listAgentIds();
            for (int i = 0; i < ids.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                AgentProfile profile = agentRegistry.load(ids.get(i));
                AgentStatus status = profile == null
                        ? AgentStatus.parse("", config)
                        : AgentStatus.parse(readIfExists(profile.statusFile()), config);
                AgentRuntime runtime = profile == null
                        ? AgentRuntime.initial(ids.get(i), status.executionState())
                        : new AgentRuntimeStore().read(profile, status.executionState());
                json.append("{")
                        .append("\"id\":\"").append(TextUtils.escapeJson(ids.get(i))).append("\",")
                        .append("\"executionState\":\"").append(TextUtils.escapeJson(status.executionState())).append("\",")
                        .append("\"runtimeState\":\"").append(TextUtils.escapeJson(runtime.runtimeState())).append("\",")
                        .append("\"consecutiveFailures\":").append(runtime.consecutiveFailures()).append(',')
                        .append("\"totalSuccesses\":").append(runtime.totalSuccesses()).append(',')
                        .append("\"lastErrorMessage\":\"").append(TextUtils.escapeJson(runtime.lastErrorMessage())).append("\"")
                        .append("}");
            }
            json.append("]}");
            sendJson(exchange, 200, json.toString());
        });

        server.createContext("/api/agent", new AgentHandler());
        server.createContext("/api/agent-settings", new AgentSettingsHandler());
        server.createContext("/api/agent-file", new AgentFileHandler());
        server.createContext("/api/distilled", new DistilledFilesHandler());
        server.createContext("/api/distilled/file", new DistilledFileHandler());
        server.createContext("/api/company-sources", new CompanySourcesHandler());
        server.createContext("/api/usage", new UsageHandler());
        server.createContext("/api/respond", new RespondHandler());
    }

    private final class AgentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            Map<String, String> query = parseQuery(exchange.getRequestURI());
            String agentId = query.getOrDefault("id", "").trim();
            AgentProfile profile = agentRegistry.load(agentId);
            if (profile == null) {
                sendJson(exchange, 404, "{\"error\":\"Unknown agent\"}");
                return;
            }

            String role = readIfExists(profile.roleFile());
            String distill = readIfExists(profile.distillFile());
            AgentStatus status = AgentStatus.parse(readIfExists(profile.statusFile()), config);
            AgentRuntime runtime = new AgentRuntimeStore().read(profile, status.executionState());
            String distilled = new DirectoryCorpus(true, profile.distilledDir(), 8000, 3).loadContext();
            String workspace = new DirectoryCorpus(true, profile.workspaceDir(), 8000, 3).loadContext();

            String json = "{"
                    + "\"id\":\"" + TextUtils.escapeJson(profile.id()) + "\","
                    + "\"role\":\"" + TextUtils.escapeJson(role) + "\","
                    + "\"distill\":\"" + TextUtils.escapeJson(distill) + "\","
                    + "\"status\":\"" + TextUtils.escapeJson(status.renderedStatus()) + "\","
                    + "\"executionState\":\"" + TextUtils.escapeJson(status.executionState()) + "\","
                    + "\"runtime\":\"" + TextUtils.escapeJson(runtime.render()) + "\","
                    + "\"distilled\":\"" + TextUtils.escapeJson(distilled) + "\","
                    + "\"workspace\":\"" + TextUtils.escapeJson(workspace) + "\""
                    + "}";
            sendJson(exchange, 200, json);
        }
    }

    private final class AgentFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            AgentProfile profile = requireAgent(exchange);
            if (profile == null) {
                return;
            }
            Map<String, String> query = parseQuery(exchange.getRequestURI());
            String type = query.getOrDefault("type", "").trim();
            Path path = switch (type) {
                case "role" -> profile.roleFile();
                case "status" -> profile.statusFile();
                case "distill" -> profile.distillFile();
                default -> null;
            };
            if (path == null) {
                sendJson(exchange, 400, "{\"error\":\"Unknown agent file type\"}");
                return;
            }
            String method = exchange.getRequestMethod().toUpperCase();
            if ("GET".equals(method)) {
                String content = readIfExists(path);
                String json = "{"
                        + "\"type\":\"" + TextUtils.escapeJson(type) + "\","
                        + "\"content\":\"" + TextUtils.escapeJson(content) + "\""
                        + "}";
                sendJson(exchange, 200, json);
                return;
            }
            if ("PUT".equals(method)) {
                Map<String, String> body = parseJsonObject(readBody(exchange));
                String content = body.getOrDefault("content", "");
                writeTextFile(path, content);
                String json = "{"
                        + "\"type\":\"" + TextUtils.escapeJson(type) + "\","
                        + "\"saved\":true"
                        + "}";
                sendJson(exchange, 200, json);
                return;
            }
            sendMethodNotAllowed(exchange);
        }
    }

    private final class AgentSettingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            AgentProfile profile = requireAgent(exchange);
            if (profile == null) {
                return;
            }
            if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            Map<String, String> body = parseJsonObject(readBody(exchange));
            String executionState = body.getOrDefault("executionState", "").trim();
            int runIntervalSeconds = parsePositiveInt(body.getOrDefault("runIntervalSeconds", ""), 300);
            new AgentStatusStore(config).updateSettings(profile, executionState, runIntervalSeconds);
            AgentStatus status = AgentStatus.parse(readIfExists(profile.statusFile()), config);
            String json = "{"
                    + "\"saved\":true,"
                    + "\"executionState\":\"" + TextUtils.escapeJson(status.executionState()) + "\","
                    + "\"runIntervalSeconds\":" + status.runIntervalSeconds()
                    + "}";
            sendJson(exchange, 200, json);
        }
    }

    private final class DistilledFilesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            AgentProfile profile = requireAgent(exchange);
            if (profile == null) {
                return;
            }
            Files.createDirectories(profile.distilledDir());
            List<Path> files = listMarkdownFiles(profile.distilledDir());
            StringBuilder json = new StringBuilder();
            json.append("{\"files\":[");
            for (int i = 0; i < files.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                json.append("{\"name\":\"")
                        .append(TextUtils.escapeJson(files.get(i).getFileName().toString()))
                        .append("\"}");
            }
            json.append("]}");
            sendJson(exchange, 200, json.toString());
        }
    }

    private final class DistilledFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            AgentProfile profile = requireAgent(exchange);
            if (profile == null) {
                return;
            }
            Map<String, String> query = parseQuery(exchange.getRequestURI());
            String rawName = query.getOrDefault("name", "").trim();
            String name = sanitizeFileName(rawName);
            if (name.isBlank()) {
                sendJson(exchange, 400, "{\"error\":\"File name is required\"}");
                return;
            }
            Path path = profile.distilledDir().resolve(name);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("GET".equals(method)) {
                if (!Files.exists(path)) {
                    sendJson(exchange, 404, "{\"error\":\"File not found\"}");
                    return;
                }
                String content = Files.readString(path, StandardCharsets.UTF_8);
                String json = "{"
                        + "\"name\":\"" + TextUtils.escapeJson(name) + "\","
                        + "\"content\":\"" + TextUtils.escapeJson(content) + "\""
                        + "}";
                sendJson(exchange, 200, json);
                return;
            }
            if ("PUT".equals(method)) {
                Map<String, String> body = parseJsonObject(readBody(exchange));
                String content = body.getOrDefault("content", "");
                Files.createDirectories(profile.distilledDir());
                Files.writeString(
                        path,
                        content,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE);
                String json = "{"
                        + "\"name\":\"" + TextUtils.escapeJson(name) + "\","
                        + "\"saved\":true"
                        + "}";
                sendJson(exchange, 200, json);
                return;
            }
            sendMethodNotAllowed(exchange);
        }
    }

    private final class CompanySourcesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(method)) {
                sendJson(exchange, 200, renderCompanySourcesJson());
                return;
            }
            if ("POST".equals(method)) {
                Map<String, String> body = parseJsonObject(readBody(exchange));
                companyDataSources.add(Path.of(body.getOrDefault("path", "")));
                sendJson(exchange, 200, renderCompanySourcesJson());
                return;
            }
            if ("PUT".equals(method) || "DELETE".equals(method)) {
                String suffix = path.substring("/api/company-sources".length()).trim();
                if (suffix.startsWith("/")) {
                    suffix = suffix.substring(1);
                }
                int index = Integer.parseInt(suffix) - 1;
                if ("PUT".equals(method)) {
                    Map<String, String> body = parseJsonObject(readBody(exchange));
                    companyDataSources.update(index, Path.of(body.getOrDefault("path", "")));
                } else {
                    companyDataSources.remove(index);
                }
                sendJson(exchange, 200, renderCompanySourcesJson());
                return;
            }
            sendMethodNotAllowed(exchange);
        }
    }

    private final class RespondHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            Map<String, String> body = parseJsonObject(readBody(exchange));
            String agentId = body.getOrDefault("agentId", "");
            AgentMode mode = AgentMode.fromCliArg(body.getOrDefault("mode", "general"));
            String prompt = body.getOrDefault("prompt", "").trim();
            if (prompt.isBlank()) {
                sendJson(exchange, 400, "{\"error\":\"Prompt is required\"}");
                return;
            }

            try {
                String response = agent.respond(new AgentRequest(mode, prompt, agentId));
                String json = "{\"response\":\"" + TextUtils.escapeJson(response) + "\"}";
                sendJson(exchange, 200, json);
            } catch (Exception ex) {
                String json = "{\"error\":\"" + TextUtils.escapeJson(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()) + "\"}";
                sendJson(exchange, 500, json);
            }
        }
    }

    private final class UsageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            List<UsageSummary> summaries = ProviderFactory.createUsageTracker(config).summarize();
            StringBuilder json = new StringBuilder();
            json.append("{\"usage\":[");
            for (int i = 0; i < summaries.size(); i++) {
                UsageSummary summary = summaries.get(i);
                if (i > 0) {
                    json.append(',');
                }
                json.append('{')
                        .append("\"provider\":\"").append(TextUtils.escapeJson(summary.provider())).append("\",")
                        .append("\"model\":\"").append(TextUtils.escapeJson(summary.model())).append("\",")
                        .append("\"dailyTotalTokens\":").append(summary.dailyTotalTokens()).append(',')
                        .append("\"monthlyTotalTokens\":").append(summary.monthlyTotalTokens()).append(',')
                        .append("\"yearlyTotalTokens\":").append(summary.yearlyTotalTokens()).append(',')
                        .append("\"requests\":").append(summary.requests()).append(',')
                        .append("\"allExact\":").append(summary.allExact())
                        .append('}');
            }
            json.append("]}");
            sendJson(exchange, 200, json.toString());
        }
    }

    private String renderCompanySourcesJson() throws IOException {
        List<Path> paths = companyDataSources.list();
        StringBuilder json = new StringBuilder();
        json.append("{\"sources\":[");
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"index\":").append(i + 1).append(",\"path\":\"")
                    .append(TextUtils.escapeJson(paths.get(i).toString())).append("\"}");
        }
        json.append("]}");
        return json.toString();
    }

    private static String readIfExists(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return "";
        }
        return Files.readString(path).trim();
    }

    private static void writeTextFile(Path path, String content) throws IOException {
        if (path == null) {
            return;
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(
                path,
                content == null ? "" : content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> query = new HashMap<>();
        String raw = uri.getRawQuery();
        if (raw == null || raw.isBlank()) {
            return query;
        }
        for (String part : raw.split("&")) {
            String[] pieces = part.split("=", 2);
            String key = pieces[0];
            String value = pieces.length > 1 ? TextUtils.decodeUrl(pieces[1]) : "";
            query.put(key, value);
        }
        return query;
    }

    private static Map<String, String> parseJsonObject(String json) {
        Map<String, String> values = new HashMap<>();
        String text = json == null ? "" : json.trim();
        if (text.isEmpty()) {
            return values;
        }
        int index = 0;
        while (index < text.length()) {
            int keyStart = text.indexOf('"', index);
            if (keyStart < 0) {
                break;
            }
            String key = TextUtils.readJsonStringValue(text, keyStart + 1);
            int keyEnd = findClosingQuote(text, keyStart + 1);
            if (keyEnd < 0) {
                break;
            }
            int colon = text.indexOf(':', keyEnd + 1);
            if (colon < 0) {
                break;
            }
            int valueStart = text.indexOf('"', colon + 1);
            if (valueStart < 0) {
                break;
            }
            String value = TextUtils.readJsonStringValue(text, valueStart + 1);
            int valueEnd = findClosingQuote(text, valueStart + 1);
            if (valueEnd < 0) {
                break;
            }
            values.put(key, value);
            index = valueEnd + 1;
        }
        return values;
    }

    private static int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private AgentProfile requireAgent(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI());
        String agentId = query.getOrDefault("id", "").trim();
        AgentProfile profile = agentRegistry.load(agentId);
        if (profile == null) {
            sendJson(exchange, 404, "{\"error\":\"Unknown agent\"}");
            return null;
        }
        return profile;
    }

    private static List<Path> listMarkdownFiles(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return List.of();
        }
        List<Path> files = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(files::add);
        }
        return files;
    }

    private static String sanitizeFileName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String name = Path.of(raw).getFileName().toString().trim();
        if (name.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                out.append(c);
            } else if (Character.isWhitespace(c)) {
                out.append('_');
            }
        }
        String sanitized = out.toString();
        if (sanitized.isBlank()) {
            return "";
        }
        if (!sanitized.endsWith(".md")) {
            sanitized = sanitized + ".md";
        }
        return sanitized;
    }

    private static int findClosingQuote(String text, int start) {
        boolean escaping = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static void sendHtml(HttpExchange exchange, String html) throws IOException {
        send(exchange, 200, "text/html; charset=utf-8", html);
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", json);
    }

    private static void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
