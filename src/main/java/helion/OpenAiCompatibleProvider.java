package helion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenAiCompatibleProvider implements LlmProvider {
    private static final Pattern CONTENT_PATTERN =
            Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"");
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
    public String chat(List<LlmMessage> messages) throws IOException, InterruptedException {
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

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(providerName + " API error: HTTP " + response.statusCode() + " " + response.body());
        }
        return extractAssistantContent(response.body());
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
        Matcher matcher = CONTENT_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            return "";
        }
        return TextUtils.unescapeJson(matcher.group(1));
    }
}
