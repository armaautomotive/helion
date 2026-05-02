package armadesignstudio.gen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class LlamaCppClient {
    private final String baseUrl;

    public LlamaCppClient() {
        this("http://localhost:8080");
    }

    public LlamaCppClient(String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean isAvailable() {
        try {
            HttpURLConnection conn = openConnection("/v1/models", "GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(3000);
            int status = conn.getResponseCode();
            conn.disconnect();
            return status == 200;
        } catch (Exception ex) {
            return false;
        }
    }

    public List<String> listModels() throws Exception {
        try {
            HttpURLConnection conn = openConnection("/v1/models", "GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            int status = conn.getResponseCode();
            String body = readBody(conn, status);
            conn.disconnect();
            if (status < 200 || status >= 300) {
                throw new IOException("llama.cpp models API error at " + baseUrl + "/v1/models: HTTP " + status + " " + body);
            }

            List<String> models = new ArrayList<String>();
            JSONObject json = new JSONObject(body);
            JSONArray data = json.optJSONArray("data");
            if (data != null) {
                for (int i = 0; i < data.length(); i++) {
                    JSONObject model = data.optJSONObject(i);
                    if (model == null) {
                        continue;
                    }
                    String id = model.optString("id", "").trim();
                    if (id.length() > 0) {
                        models.add(id);
                    }
                }
            }
            return models;
        } catch (IOException ex) {
            throw new IOException("Unable to reach llama.cpp server at " + baseUrl + ". " + ex.getMessage(), ex);
        }
    }

    public String chat(String model, String userMessage) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", new JSONArray().put(new JSONObject()
                .put("role", "user")
                .put("content", userMessage)));
        body.put("stream", false);
        JSONObject response = createChatCompletion(body);
        JSONObject message = response.optJSONArray("choices") == null
                ? null
                : response.getJSONArray("choices").optJSONObject(0);
        if (message == null) {
            return "";
        }
        JSONObject assistant = message.optJSONObject("message");
        return assistant == null ? "" : assistant.optString("content", "");
    }

    public JSONObject createChatCompletion(JSONObject body) throws Exception {
        try {
            HttpURLConnection conn = openConnection("/v1/chat/completions", "POST");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int status = conn.getResponseCode();
            String responseBody = readBody(conn, status);
            conn.disconnect();
            if (status < 200 || status >= 300) {
                throw new IOException("llama.cpp chat API error at " + baseUrl + "/v1/chat/completions: HTTP " + status + " " + responseBody);
            }
            return new JSONObject(responseBody);
        } catch (IOException ex) {
            throw new IOException("Unable to reach llama.cpp server at " + baseUrl + ". " + ex.getMessage(), ex);
        }
    }

    private HttpURLConnection openConnection(String path, String method) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        return conn;
    }

    private String readBody(HttpURLConnection conn, int status) throws Exception {
        InputStream stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (stream == null) {
            return "";
        }
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private static String normalizeBaseUrl(String url) {
        String value = url == null ? "" : url.trim();
        if (value.length() == 0) {
            value = "http://localhost:8000";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
