package armadesignstudio;

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

public class Google {
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
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

        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("x-goog-api-key", apiKey);
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
            throw new IOException("Google Models API error: HTTP " + status + " " + response);
        }

        JSONObject root = new JSONObject(response.toString());
        JSONArray models = root.optJSONArray("models");
        List<String> modelNames = new ArrayList<String>();
        if(models != null){
            for(int i = 0; i < models.length(); i++){
                JSONObject model = models.getJSONObject(i);
                String name = model.optString("name", "");
                if(name.startsWith("models/")){
                    name = name.substring("models/".length());
                }
                if(name.startsWith("gemini") && name.indexOf("embedding") == -1){
                    modelNames.add(name);
                }
            }
        }
        return modelNames;
    }

    public static String chat(String prompt, String model) throws IOException {
        JSONObject body = new JSONObject();
        body.put("contents", new JSONArray().put(new JSONObject()
                .put("role", "user")
                .put("parts", new JSONArray().put(new JSONObject().put("text", prompt)))));
        JSONObject response = generateContent(model, body);
        return extractText(response);
    }

    public static JSONObject generateContent(String model, JSONObject body) throws IOException {
        load();
        if(apiKey == null || apiKey.length() == 0){
            throw new IOException("Google API-KEY Missing.");
        }
        String actualModel = (model == null || model.length() == 0) ? DEFAULT_MODEL : model;
        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + actualModel + ":generateContent");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("x-goog-api-key", apiKey);
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
            throw new IOException("Google GenerateContent API error: HTTP " + status + " " + response);
        }
        return new JSONObject(response.toString());
    }

    public static String extractText(JSONObject response) {
        JSONArray candidates = response.optJSONArray("candidates");
        if(candidates == null || candidates.length() == 0){
            return "";
        }
        JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
        if(content == null){
            return "";
        }
        JSONArray parts = content.optJSONArray("parts");
        if(parts == null){
            return "";
        }
        StringBuilder text = new StringBuilder();
        for(int i = 0; i < parts.length(); i++){
            JSONObject part = parts.optJSONObject(i);
            if(part != null && part.has("text")){
                if(text.length() > 0){
                    text.append("\n");
                }
                text.append(part.optString("text", ""));
            }
        }
        return text.toString();
    }

    public static void promptForAPIKEY(){
        load();
        ImageIcon iconImage = new ImageIcon(Google.class.getResource("/armadesignstudio/Icons/favicon-32x32.png"));
        apiKey = (String) JOptionPane.showInputDialog(null, "Enter Google AI API Key", "Google API KEY", JOptionPane.QUESTION_MESSAGE, iconImage, null, apiKey);
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
            prop.setProperty("google.api_key", apiKey);
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
            apiKey = prop.getProperty("google.api_key");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
