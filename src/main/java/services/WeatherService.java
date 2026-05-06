package services;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherService {

    private static final String API_URL = "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current=temperature_2m,relative_humidity_2m,rain,wind_speed_10m,weather_code&daily=temperature_2m_max,temperature_2m_min,rain_sum,wind_speed_10m_max,weather_code&timezone=auto";

    public JSONObject getWeatherData(double latitude, double longitude) {
        try {
            String urlString = String.format(java.util.Locale.US, API_URL, latitude, longitude);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

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
                System.err.println("GET request failed. Response Code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getWeatherIcon(int code) {
        // Simple mapping of WMO Weather interpretation codes
        if (code == 0) return "\u2600\uFE0F"; // Clear sky (☀️)
        if (code >= 1 && code <= 3) return "\u26C5"; // Partly cloudy (⛅)
        if (code >= 45 && code <= 48) return "\uD83C\uDF2B\uFE0F"; // Fog (🌫️)
        if (code >= 51 && code <= 67) return "\uD83C\uDF27\uFE0F"; // Drizzle/Rain (🌧️)
        if (code >= 71 && code <= 77) return "\u2744\uFE0F"; // Snow (❄️)
        if (code >= 80 && code <= 82) return "\uD83C\uDF26\uFE0F"; // Rain showers (🌦️)
        if (code >= 95 && code <= 99) return "\u26C8\uFE0F"; // Thunderstorm (⛈️)
        return "\u2753"; // Unknown (❓)
    }

    public String getWeatherDescription(int code) {
        if (code == 0) return "Ciel dégagé";
        if (code >= 1 && code <= 3) return "Partiellement nuageux";
        if (code >= 45 && code <= 48) return "Brouillard";
        if (code >= 51 && code <= 55) return "Bruine";
        if (code >= 61 && code <= 67) return "Pluie";
        if (code >= 71 && code <= 77) return "Neige";
        if (code >= 80 && code <= 82) return "Averses";
        if (code >= 95 && code <= 99) return "Orage";
        return "Inconnu";
    }
}
