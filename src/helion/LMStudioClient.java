/**
 
 
 MacOSX start LM STudio rest server:
 /Applications/LM Studio.app/Contents/MacOS
 ~/.lmstudio/bin/lms server start
 
 */

package armadesignstudio.gen;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.net.URI;
import java.io.InputStream;
import java.io.OutputStream;
import org.json.JSONObject;

public class LMStudioClient {
    // Ports to probe — LM Studio defaults to 1234, but sometimes picks another
    private static final int[] PORTS = {1234, 1235, 1236, 1237, 1238, 1239, 1240};
    
    //private final String endpoint; // e.g. "http://localhost:1234/v1/chat/completions"
    //private final String apiKey;   // optional; LM Studio local server usually doesn't require it
    
    public LMStudioClient(){
        
    }
    
    private static final String LMSTUDIO_URL = "http://localhost:1234/v1/models";

    public boolean isLMStudioRestAvailable() {
        try {
            URL url = new URL(LMSTUDIO_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000); // 2 seconds
            conn.setReadTimeout(2000);

            int responseCode = conn.getResponseCode();
            return (responseCode == 200);
        } catch (IOException e) {
            return false;
        }
    }
    
    // Heuristic substrings that identify LM Studio's processes on different OSes.
    // macOS is an Electron bundle: ".../LM Studio.app/Contents/MacOS/LM Studio"
    // Helpers like "LM Studio Helper (Renderer)" may show as separate processes.
    private static final List<String> NAME_HINTS = Arrays.asList(
            "LM Studio.app/Contents/MacOS/LM Studio", // macOS main binary path
            "LM Studio Helper",                        // macOS Electron helpers
            File.separator + "LM Studio" + File.separator, // generic path hint
            File.separator + "LMStudio" + File.separator,  // alt packaging
            "lm-studio", "lmstudio"                      // linux package names (just in case)
    );

    /**
     * Returns true if any running process appears to be LM Studio.
     * No network access is used.
     */
    public boolean isLMStudioRunning() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        boolean isMac = os.contains("mac");
        boolean isWin = os.contains("win");

        for (ProcessHandle ph : ProcessHandle.allProcesses().toArray(ProcessHandle[]::new)) {
            try {
                ProcessHandle.Info info = ph.info();
                // Command (full path to executable) is the most reliable on macOS/Linux.
                String cmd = info.command().orElse("").toLowerCase(Locale.ROOT);
                // Arguments sometimes include the app/renderer names.
                String args = String.join(" ", info.arguments().orElse(new String[0]))
                                   .toLowerCase(Locale.ROOT);

                if (cmd.isEmpty() && args.isEmpty()) continue;

                if (looksLikeLMStudio(cmd, args, isMac, isWin)) {
                    return true;
                }
            } catch (Throwable ignored) {
                // Ignore permission or transient errors and keep scanning
            }
        }
        return false;
    }

    private boolean looksLikeLMStudio(String cmd, String args, boolean isMac, boolean isWin) {
        // Quick OS-specific hints
        if (isMac) {
            // Most common macOS bundle path and helper names
            if (cmd.contains("lm studio.app/contents/macos/lm studio")) return true;
            if (cmd.contains("lm studio helper")) return true;
            if (args.contains("lm studio helper")) return true;
        }
        if (isWin) {
            // Windows installs may show command paths with spaces
            if (cmd.contains("\\lm studio\\") || cmd.endsWith("lm studio.exe")) return true;
        }

        // Cross-platform fallback: any of our generic hints in either the command or args
        for (String hint : NAME_HINTS) {
            String h = hint.toLowerCase(Locale.ROOT);
            if (cmd.contains(h) || args.contains(h)) return true;
        }
        return false;
    }

    /**
     * (Optional) Try to start the LM Studio local REST server via the CLI,
     * but ONLY if LM Studio is already running (so we don’t spawn it headless
     * when the user expects the GUI app).
     *
     * This does NOT block; it spawns the CLI and returns.
     * You can read the process output yourself if you want to confirm the port.
     */
    public Process startLocalServerIfLMStudioRunning() throws IOException {
        if (!isLMStudioRunning()) return null;

        // macOS typical CLI locations first (adjust/fill as needed)
        List<String> candidates = new ArrayList<>();

        // User bootstrap location (recommended)
        candidates.add(System.getProperty("user.home") + "/.lmstudio/bin/lms");

        // PATH fallback: rely on 'lms' being in PATH
        candidates.add("lms");

        // Directly inside the app bundle (some builds ship it here)
        candidates.add("/Applications/LM Studio.app/Contents/MacOS/lms");
        candidates.add("/Applications/LM Studio.app/Contents/Resources/bin/lms");

        // Try each candidate until one starts successfully
        IOException lastError = null;
        for (String cli : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cli, "server", "start");
                pb.redirectErrorStream(true);
                return pb.start();
            } catch (IOException e) {
                lastError = e; // remember and try next candidate
            }
        }
        if (lastError != null) throw lastError;
        return null;
    }

    
    
    /** Returns the first reachable LM Studio base URL (http://localhost:PORT). */
    private static String findServerBaseUrl() {
        for (int port : PORTS) {
            String base = "http://localhost:" + port;
            if (isReachable(base + "/v1/models")) {
                return base;
            }
        }
        return null;
    }

    /** Quick GET to see if endpoint responds with 200. */
    private static boolean isReachable(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(1000);
            c.setReadTimeout(2000);
            return c.getResponseCode() == 200;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Fetches /v1/models and extracts model IDs with a regex.
     *
     */
    public static List<String> listModels() throws Exception {
        String base = findServerBaseUrl();
        if (base == null) {
            throw new IllegalStateException("LM Studio server not reachable on ports 1234–1240.");
        }
        HttpURLConnection c = (HttpURLConnection) new URL(base + "/v1/models").openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(1500);
        c.setReadTimeout(3000);
        if (c.getResponseCode() != 200) {
            throw new IllegalStateException("Unexpected HTTP " + c.getResponseCode());
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        String json = sb.toString();
        //System.out.println("json " + json);
        // Regex to extract: "id":"some-model-name"
        Pattern p = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        List<String> ids = new ArrayList<>();
        while (m.find()) {
            String modelId = m.group(1);
            if( modelId.indexOf("embedding") == -1 ){ // ignore non language models.
                ids.add(modelId);
            }
        }
        return ids;
    }
    
    
    
    public static List<String> getModels() throws Exception {
        String base = findServer();
        if (base == null) {
            throw new IllegalStateException("LM Studio server not reachable on localhost ports 1234–1238.");
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(base + "/v1/models").openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(1500);
        conn.setReadTimeout(3000);

        if (conn.getResponseCode() != 200) {
            throw new IllegalStateException("HTTP " + conn.getResponseCode() + " from " + base);
        }

        // Read response
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line);
            }
        }
        String json = sb.toString();
        
        System.out.println(":" + json); // ***

        // Extract model ids with regex
        List<String> models = new ArrayList<>();
        Matcher m = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        while (m.find()) {
            models.add(m.group(1));
        }
        return models;
    }
    
    private static String findServer() {
        for (int port : PORTS) {
            String url = "http://localhost:" + port;
            if (reachable(url + "/v1/models")) {
                return url;
            }
        }
        return null;
    }

    private static boolean reachable(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(500);
            c.setReadTimeout(1000);
            return c.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    
    
    /**
     * Sends a user message and returns the assistant's reply as a String.
     * @param model  The local model name as shown in LM Studio (e.g., "granite-7b-instruct-q4_0")
     * @param userMessage  Your chat prompt
     */
    public String chat(String model, String userMessage) throws Exception {
        String payload = buildJson(model, userMessage);
        
        //System.out.println("payload: " + payload);

        String base = findServerBaseUrl();
        if (base == null) {
            throw new IllegalStateException("LM Studio server not reachable on ports 1234–1240.");
        }

        StringBuilder response = new StringBuilder();
        int status;
        try {
            URL url = new URL(base + "/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);
            conn.setDoOutput(true);

            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            conn.disconnect();
        } catch (java.net.SocketTimeoutException ex) {
            throw new RuntimeException("LM Studio request timed out. Verify the selected model is loaded in LM Studio and that the local server is responding. " + ex.getMessage(), ex);
        }

        if (status / 100 != 2) {
            throw new RuntimeException("LM Studio HTTP " + status + ": " + response.toString());
        }

        //System.out.println("response: " + response.toString());
        
        return extractAssistantContent(response.toString());
    }

    public JSONObject createChatCompletion(JSONObject body) throws Exception {
        String base = findServerBaseUrl();
        if (base == null) {
            throw new IllegalStateException("LM Studio server not reachable on ports 1234–1240.");
        }

        String payload = body.toString();
        StringBuilder response = new StringBuilder();
        int status;
        try {
            URL url = new URL(base + "/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            conn.disconnect();
        } catch (java.net.SocketTimeoutException ex) {
            throw new RuntimeException("LM Studio request timed out. Verify the selected model is loaded in LM Studio and that the local server is responding. " + ex.getMessage(), ex);
        }

        if (status / 100 != 2) {
            throw new RuntimeException("LM Studio HTTP " + status + ": " + response.toString());
        }
        return new JSONObject(response.toString());
    }

    // --- helpers: tiny JSON builder & minimal extractor (no external deps) ---

    private static String buildJson(String model, String userMessage) {
        // stream=false to get a single JSON response (easier to parse here)
        return "{"
                + "\"model\":\"" + jsonEscape(model) + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + jsonEscape(userMessage) + "\"}],"
                + "\"temperature\":0.7,"
                + "\"stream\":false"
                + "}";
    }

    private static String extractAssistantContent(String json) {
        int i = indexAfter(json, "\"choices\"");
        if (i < 0) return fallback(json);

        i = indexAfter(json, "\"message\"", i);
        if (i < 0) return fallback(json);

        i = indexAfter(json, "\"content\"", i);
        if (i < 0) return fallback(json);

        i = indexAfter(json, ":", i);
        if (i < 0) return fallback(json);

        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length() || json.charAt(i) != '\"') return fallback(json);

        int start = i + 1;
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int j = start; j < json.length(); j++) {
            char c = json.charAt(j);
            if (escaped) {
                switch (c) {
                    case '\"': sb.append('\"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/');  break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'u':
                        if (j + 4 < json.length()) {
                            String hex = json.substring(j + 1, j + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                j += 4;
                            } catch (NumberFormatException e) {
                                sb.append("\\u").append(hex);
                                j += 4;
                            }
                        } else {
                            sb.append("\\u");
                        }
                        break;
                    default:
                        sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '\"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return fallback(json);
    }

    private static int indexAfter(String s, String token) {
        int k = s.indexOf(token);
        return (k < 0) ? -1 : k + token.length();
    }

    private static int indexAfter(String s, String token, int fromIndex) {
        int k = s.indexOf(token, fromIndex);
        return (k < 0) ? -1 : k + token.length();
        }

    private static String fallback(String json) {
        // last-ditch: return raw JSON if we couldn't parse
        return json;
    }

    private static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
    
}
