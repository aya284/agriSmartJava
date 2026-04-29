package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiService {
    private static final String API_KEY = "AIzaSyBHCnpS6wEptcgRgTNezIJ4kDP0cNPTfAw";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    public String generateRecommendation(String prompt) throws Exception {
        JSONObject requestJson = new JSONObject();
        
        JSONObject message = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);
        parts.put(textPart);
        message.put("parts", parts);
        
        JSONArray contents = new JSONArray();
        contents.put(message);
        
        requestJson.put("contents", contents);

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(20000);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestJson.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject content = firstCandidate.getJSONObject("content");
                JSONArray resParts = content.getJSONArray("parts");
                if (resParts.length() > 0) {
                    return resParts.getJSONObject(0).getString("text");
                }
            }
            return "Aucune recommandation générée.";
        } else {
            BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = err.readLine()) != null) {
                errorResponse.append(line);
            }
            err.close();
            throw new Exception("Erreur API Gemini (" + responseCode + "): " + errorResponse.toString());
        }
    }
}
