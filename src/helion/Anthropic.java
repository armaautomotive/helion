package helion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import org.json.JSONArray;
import org.json.JSONObject;

public class Anthropic {
    private static final String API_VERSION = "2023-06-01";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";
    private static String apiKey;

    public static boolean hasApiKey() {
        load();
        return apiKey != null && apiKey.length() > 0;
    }

    public static List<String> listModels() throws IOException {
        load();
        if(apiKey == null || apiKey.length() == 0){
            return null;
        }

        URL url = new URL("https://api.anthropic.com/v1/models");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("x-api-key", apiKey);
        connection.setRequestProperty("anthropic-version", API_VERSION);
        connection.setRequestProperty("Content-Type", "application/json");

        int status = connection.getResponseCode();
        InputStream stream = (status >= 200 && status < 300) ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        if(status < 200 || status >= 300){
            throw new IOException("Anthropic Models API error: HTTP " + status + " " + response);
        }

        JSONObject root = new JSONObject(response.toString());
        JSONArray data = root.optJSONArray("data");
        List<String> modelNames = new ArrayList<String>();
        if(data != null){
            for(int i = 0; i < data.length(); i++){
                JSONObject model = data.getJSONObject(i);
                modelNames.add(model.getString("id"));
            }
        }
        return modelNames;
    }

    public static String chat(String prompt, String model) throws IOException {
        JSONObject body = new JSONObject();
        body.put("model", model == null || model.length() == 0 ? DEFAULT_MODEL : model);
        body.put("max_tokens", 1024);
        body.put("messages", new JSONArray().put(new JSONObject()
                .put("role", "user")
                .put("content", prompt)));
        JSONObject response = createMessage(body);
        return extractText(response);
    }

    public static JSONObject createMessage(JSONObject body) throws IOException {
        load();
        if(apiKey == null || apiKey.length() == 0){
            throw new IOException("Anthropic API-KEY Missing.");
        }

        URL url = new URL("https://api.anthropic.com/v1/messages");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("x-api-key", apiKey);
        connection.setRequestProperty("anthropic-version", API_VERSION);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
            writer.write(body.toString());
            writer.flush();
        }

        int status = connection.getResponseCode();
        InputStream stream = (status >= 200 && status < 300) ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        if(status < 200 || status >= 300){
            throw new IOException("Anthropic Messages API error: HTTP " + status + " " + response);
        }
        return new JSONObject(response.toString());
    }

    public static String extractText(JSONObject response) {
        JSONArray content = response.optJSONArray("content");
        if(content == null){
            return "";
        }
        StringBuilder text = new StringBuilder();
        for(int i = 0; i < content.length(); i++){
            JSONObject block = content.optJSONObject(i);
            if(block != null && "text".equals(block.optString("type"))){
                if(text.length() > 0){
                    text.append("\n");
                }
                text.append(block.optString("text", ""));
            }
        }
        return text.toString();
    }

    public static void promptForAPIKEY(){
        load();
        ImageIcon iconImage = new ImageIcon(Anthropic.class.getResource("/armadesignstudio/Icons/favicon-32x32.png"));
        apiKey = (String) JOptionPane.showInputDialog(null, "Enter Anthropic API Key", "Anthropic API KEY", JOptionPane.QUESTION_MESSAGE, iconImage, null, apiKey);
        if(apiKey != null && apiKey.length() > 0){
            save();
        }
    }

    public static void save(){
        Properties prop = new Properties();
        try {
            String path = Settings.getSettingsPath();
            String propertyFileName = path + System.getProperty("file.separator") + "ads.properties";

            File f = new File(propertyFileName);
            if(f.exists()){
                InputStream input = new FileInputStream(propertyFileName);
                prop.load(input);
            }
            OutputStream output = new FileOutputStream(propertyFileName);
            prop.setProperty("anthropic.api_key", apiKey);
            prop.store(output, null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void load(){
        try {
            String path = Settings.getSettingsPath();
            String propertyFileName = path + System.getProperty("file.separator") + "ads.properties";

            File f = new File(propertyFileName);
            if(f.exists() == false){
                return;
            }

            InputStream input = new FileInputStream(propertyFileName);
            Properties prop = new Properties();
            prop.load(input);
            apiKey = prop.getProperty("anthropic.api_key");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
