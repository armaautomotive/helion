package helion;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class OpenAiCompatibleProvider implements LlmProvider {
    private final HttpClient httpClient;
    private final String providerName;
    private final String endpoint;
    private final String model;
    private final String authorizationHeader;

    public OpenAiCompatibleProvider(String providerName, String endpoint, String model, String authorizationHeader) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.providerName = providerName;
        this.endpoint = endpoint;
        this.model = model;
        this.authorizationHeader = authorizationHeader;
    }

    @Override
    public String name() {
        return providerName;
    }

    @Override
    public String modelName() {
        return model;
    }

    @Override
    public LlmResult chatResult(List<LlmMessage> messages) throws IOException, InterruptedException {
        String body = buildBody(messages);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json");
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            builder.header("Authorization", authorizationHeader);
        }

        HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (ConnectException ex) {
            throw new IOException(providerName + " endpoint unavailable: " + endpoint, ex);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(providerName + " API error: HTTP " + response.statusCode() + " " + response.body());
        }
        String responseBody = response.body();
        String content = extractAssistantContent(responseBody);
        UsageMetrics usage = extractUsage(responseBody, messages, content);
        return new LlmResult(content, usage);
    }

    private String buildBody(List<LlmMessage> messages) {
        StringBuilder body = new StringBuilder();
        body.append("{\"model\":\"").append(TextUtils.escapeJson(model)).append("\",\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            LlmMessage message = messages.get(i);
            if (i > 0) {
                body.append(',');
            }
            body.append("{\"role\":\"")
                    .append(TextUtils.escapeJson(message.role()))
                    .append("\",\"content\":\"")
                    .append(TextUtils.escapeJson(message.content()))
                    .append("\"}");
        }
        body.append("]}");
        return body.toString();
    }

    public static String extractAssistantContent(String responseBody) {
        String marker = "\"content\":\"";
        int start = responseBody.indexOf(marker);
        if (start < 0) {
            marker = "\"content\": \"";
            start = responseBody.indexOf(marker);
        }
        if (start < 0) {
            return "";
        }
        start += marker.length();
        return TextUtils.readJsonStringValue(responseBody, start);
    }

    private UsageMetrics extractUsage(String responseBody, List<LlmMessage> messages, String content) {
        Integer prompt = extractIntField(responseBody, "prompt_tokens");
        Integer completion = extractIntField(responseBody, "completion_tokens");
        Integer total = extractIntField(responseBody, "total_tokens");
        if (prompt != null && completion != null && total != null) {
            return new UsageMetrics(providerName, model, prompt, completion, total, true);
        }
        int estimatedPrompt = estimateTokens(joinMessages(messages));
        int estimatedCompletion = estimateTokens(content);
        return new UsageMetrics(providerName, model, estimatedPrompt, estimatedCompletion, estimatedPrompt + estimatedCompletion, false);
    }

    private static Integer extractIntField(String text, String fieldName) {
        String marker = "\"" + fieldName + "\":";
        int start = text.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
        }
        if (end == start) {
            return null;
        }
        return Integer.parseInt(text.substring(start, end));
    }

    private static String joinMessages(List<LlmMessage> messages) {
        StringBuilder out = new StringBuilder();
        for (LlmMessage message : messages) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(message.role()).append(": ").append(message.content());
        }
        return out.toString();
    }

    protected static int estimateTokens(String text) {
        String value = text == null ? "" : text;
        if (value.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(value.length() / 4.0));
    }
}
