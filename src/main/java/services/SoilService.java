package services;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SoilService {

    // Utilisation d'une API spécifique pour le SOL (ISRIC SoilGrids REST API)
    // C'est une API distincte de l'API météo.
    private static final String API_URL = "https://rest.isric.org/soilgrids/v2.0/properties/query?lon=%f&lat=%f&property=phh2o&property=sand&property=clay&depth=0-5cm&value=mean";

    public JSONObject getRealSoilData(double latitude, double longitude) {
        int maxRetries = 2; // On tente 3 fois au total
        int attempt = 0;
        
        while (attempt <= maxRetries) {
            try {
                String urlString = String.format(java.util.Locale.US, API_URL, longitude, latitude);
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                
                // On laisse encore plus de temps (60 secondes) car SoilGrids est parfois saturé
                conn.setConnectTimeout(60000); 
                conn.setReadTimeout(60000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return new JSONObject(response.toString());
                } else if (responseCode == 503 || responseCode == 504 || responseCode == 429) {
                    // Erreurs de surcharge serveur, on attend un peu avant de retenter
                    attempt++;
                    Thread.sleep(2000); 
                } else {
                    System.err.println("Soil API GET request failed. Response Code: " + responseCode);
                    break;
                }
            } catch (Exception e) {
                attempt++;
                System.err.println("Tentative " + attempt + " échouée pour SoilGrids : " + e.getMessage());
                try { Thread.sleep(2000); } catch (InterruptedException ie) {}
                if (attempt > maxRetries) {
                    System.err.println("Erreur définitive de connexion à l'API SoilGrids après 3 essais.");
                }
            }
        }
        return null;
    }
}
