/* Copyright (C) 2024 by Jon Taylor

 
 
 */
package armadesignstudio;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import javax.swing.JOptionPane;
import javax.swing.ImageIcon;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class OpenAI {
    private static String apiKey;
    private static final String DEFAULT_MODEL = "gpt-5.1";
    
    public OpenAI(){
        //load();
    }
    
    
    /**
     * proxyChatGPT
     *
     */
    public static String proxyChatGPT(String prompt){
        // https://armaautomotive.com/llm/gpt.php?prompt=how&key=
        return "Not implemented.";
    }
    
    
    /**
     * chatGPT
     * Description: Send prompt to OpenAI.
     */
    public static String chatGPT(String prompt) {
        return chatGPT(prompt, DEFAULT_MODEL);
    }

    public static String chatGPT(String prompt, String model) {
        load();
        if(apiKey == null || apiKey.length() == 0){
            // Error
            return "Error: OpenAI API-KEY Missing.";
        }
        String url = "https://api.openai.com/v1/chat/completions";

        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            // The request body
            String body = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]}";
            //System.out.println("body " + body);
            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(body);
            writer.flush();
            writer.close();

            // Response from ChatGPT
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            StringBuffer response = new StringBuffer();

            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            // calls the method to extract the message.
            return extractMessageFromJSONResponse(response.toString());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    /**
     * listModels
     * Description:
     */
    public static List<String> listModels() throws IOException, InterruptedException {
        load();
        if(apiKey == null || apiKey.length() == 0){
            // Error
            return null;
        }
        List<String> modelNames = new ArrayList<>();
        
        try {
            //String apiKey = System.getenv(apiKey); // or hardcode your key here
            URL url = new URL("https://api.openai.com/v1/models");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                //System.out.println("Available Models:");
                //System.out.println(response.toString()); // You could parse this JSON if needed
                
                // Parse
                JSONObject root = new JSONObject(response.toString());
                JSONArray dataArray = root.getJSONArray("data");

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject model = dataArray.getJSONObject(i);
                    String id = model.getString("id");
                    //modelIds.add(id);
                    
                    modelNames.add(id);
                }
                

                
            } else {
                System.err.println("Request failed: HTTP " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return modelNames;
    }

    public static boolean hasApiKey() {
        load();
        return apiKey != null && apiKey.length() > 0;
    }

    public static JSONObject createResponse(JSONObject body) throws IOException {
        load();
        if(apiKey == null || apiKey.length() == 0){
            throw new IOException("OpenAI API-KEY Missing.");
        }

        URL obj = new URL("https://api.openai.com/v1/responses");
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
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
            throw new IOException("OpenAI Responses API error: HTTP " + status + " " + response);
        }
        return new JSONObject(response.toString());
    }

    
    public static String extractMessageFromJSONResponse(String response) {
        int start = response.indexOf("content")+ 11;
        int end = response.indexOf("\"", start);
        return response.substring(start, end);
    }

    //public static void main(String[] args) {
    //    System.out.println(chatGPT("hello, how are you? Can you tell me what's a Fibonacci Number?"));
    //}


    /**
     * promptForAPIKEY
     * Description:
     */
    public static void promptForAPIKEY(){
        load();
        ImageIcon iconImage = new ImageIcon(OpenAI.class.getResource("/armadesignstudio/Icons/favicon-32x32.png"));
        //apiKey = JOptionPane.showInputDialog("Enter OpenAI API Key");
        apiKey = (String)JOptionPane.showInputDialog( null, "Enter OpenAI API Key", "OpenAI API KEY", JOptionPane.QUESTION_MESSAGE, iconImage, null, apiKey );
        if(apiKey != null && apiKey.length() > 0){
            save();
        }
    }
    
    /**
     * save
     *
     * Description: Save account information given in the input fields into the property file for persistant storage.
     */
    public static void save(){
        Properties prop = new Properties();
        try {
            String path = Settings.getSettingsPath();
            String propertyFileName = path + System.getProperty("file.separator") + "ads.properties";
            
            File f = new File(propertyFileName);
            if( f.exists() == true ) {
                InputStream input = new FileInputStream(propertyFileName);
                prop.load(input);
            }
            //String path = Settings.getSettingsPath();
            //System.out.println("path: " + path);
            
            //String propertyFileName = path + System.getProperty("file.separator") + "ads.properties";
            OutputStream output = new FileOutputStream(propertyFileName);
            
            // set the properties value
            prop.setProperty("openai.api_key", apiKey);
           
            // save properties to project root folder
            prop.store(output, null);
            //System.out.println(prop);
            
            //ImageIcon iconImage = new ImageIcon(getClass().getResource("/armadesignstudio/Icons/favicon-32x32.png"));
            
            //UIManager.put("OptionPane.minimumSize", new Dimension(230, 120));
            
            //String infoMessage = "Account saved.";
            //JOptionPane.showMessageDialog(null, infoMessage, "Account: ", JOptionPane.INFORMATION_MESSAGE, iconImage);
            
            //this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * load
     *
     * Description: Load property file attributes and populate the UI fields.
     */
    public static void load(){
        try {
            String path = Settings.getSettingsPath(); // 
            //System.out.println("path: " + path);
            String propertyFileName = path + System.getProperty("file.separator") + "ads.properties";
            
            File f = new File(propertyFileName);
            if( f.exists() == false ) {
                return;
            }
            
            InputStream input = new FileInputStream(propertyFileName);
            Properties prop = new Properties();
            // load a properties file
            prop.load(input);
            // get the property value and print it out
            //nameField.setText( prop.getProperty("ads.name") );
            apiKey = getStringProperty(prop, "openai.api_key");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * getStringProperty
     * Description:
     * @param:
     * @param:
     * @return;
     */
    public static String getStringProperty(Properties prop, String property){
        String value = prop.getProperty(property);
        return value;
    }
    
}
