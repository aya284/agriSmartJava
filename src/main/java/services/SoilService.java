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
        try {
            String urlString = String.format(java.util.Locale.US, API_URL, longitude, latitude);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(3000); // Timeout court car l'API peut être parfois lente
            conn.setReadTimeout(3000);

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
            } else {
                System.err.println("Soil API GET request failed. Response Code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Erreur de connexion à l'API SoilGrids (souvent indisponible) : " + e.getMessage());
        }
        return null;
    }
}
