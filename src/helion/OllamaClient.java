package armadesignstudio.gen;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class OllamaClient {
    private final String baseUrl;
    private final HttpClient httpClient;

    public OllamaClient() {
        this("http://localhost:11434");
    }

    public OllamaClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String generate(String model, String prompt) throws IOException, InterruptedException {
        String escapedPrompt = prompt.replace("\"", "\\\"");
        String requestBody = String.format(
            "{\n" +
            "  \"model\": \"%s\",\n" +
            "  \"prompt\": \"%s\"\n" +
            "}", model, escapedPrompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Ollama API error: " + response.statusCode() + " - " + response.body());
        }

        return response.body();
    }

    
    /**
     * chat
     * Description:
     */
    public String chat(String model, List<ChatMessage> messages) throws IOException, InterruptedException {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", buildMessagesJson(messages));
        body.put("stream", false);
        JSONObject response = createChatResponse(body);
        JSONObject message = response.optJSONObject("message");
        return message == null ? "" : stripThinkBlocks(message.optString("content", "").trim());
    }

    public JSONObject createChatResponse(JSONObject body) throws IOException, InterruptedException {
        JSONObject requestBody = new JSONObject(body.toString());
        requestBody.put("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Ollama Chat API error: " + response.statusCode() + " - " + response.body());
        }

        return new JSONObject(response.body());
    }

    private static JSONArray buildMessagesJson(List<ChatMessage> messages) {
        JSONArray messagesJson = new JSONArray();
        for (ChatMessage m : messages) {
            messagesJson.put(new JSONObject()
                    .put("role", m.role)
                    .put("content", m.content));
        }
        return messagesJson;
    }
    
    private static String stripThinkBlocks(String s) {
        // Removes <think> ... </think> blocks and similar; safe no-op if none present
        return s.replaceAll("(?s)<think>.*?</think>", "").trim();
    }

    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public static class ChatMessage {
        public String role;    // "user", "system", or "assistant"
        public String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
    
    
    /**
     * listModels
     * Description: 
     */
    public List<String> listModels() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/tags"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Ollama list models error: " + response.statusCode() + " - " + response.body());
        }

        // Parse the JSON response to extract model names
        JSONObject json = new JSONObject(response.body());
        JSONArray modelsArray = json.getJSONArray("models");
        List<String> modelNames = new ArrayList<>();

        for (int i = 0; i < modelsArray.length(); i++) {
            JSONObject model = modelsArray.getJSONObject(i);
            modelNames.add(model.getString("name"));
        }

        return modelNames;
    }
}

/**
 
 
 try {
             OllamaClient client = new OllamaClient();

             if (!client.isAvailable()) {
                 System.err.println("Ollama is not running.");
                 return;
             }

             // Chat interaction
             List<OllamaClient.ChatMessage> messages = List.of(
                 new OllamaClient.ChatMessage("system", "You are a helpful assistant."),
                 new OllamaClient.ChatMessage("user", "What's the capital of Japan?")
             );

             String result = client.chat("llama3", messages);
             System.out.println("Chat response:\n" + result);

         } catch (Exception e) {
             e.printStackTrace();
         }
 */
