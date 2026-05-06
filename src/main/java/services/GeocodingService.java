package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geocoding service using the free OpenStreetMap Nominatim API.
 * Converts addresses to GPS coordinates (latitude, longitude).
 * No API key required.
 *
 * API: https://nominatim.openstreetmap.org/search
 */
public class GeocodingService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "AgriSmart/1.0 (student-project)";

    // In-memory cache to respect rate limits (1 request/sec)
    private final Map<String, double[]> cache = new ConcurrentHashMap<>();
    private long lastRequestTime = 0;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Geocode an address to [latitude, longitude].
     *
     * @param address the address string to geocode
     * @return double array [lat, lon] or null if not found
     */
    public double[] geocode(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }

        String key = address.trim().toLowerCase();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        try {
            // Respect Nominatim rate limit: 1 request per second
            enforceRateLimit();

            String encoded = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8);
            String url = NOMINATIM_URL + "?q=" + encoded + "&format=json&limit=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONArray results = new JSONArray(response.body());
                if (!results.isEmpty()) {
                    JSONObject first = results.getJSONObject(0);
                    double lat = first.getDouble("lat");
                    double lon = first.getDouble("lon");
                    double[] coords = {lat, lon};
                    cache.put(key, coords);
                    return coords;
                }
            }
        } catch (Exception e) {
            System.err.println("Geocoding error for \"" + address + "\": " + e.getMessage());
        }

        return null;
    }

    /**
     * Calculate distance in km between two GPS coordinates using Haversine formula.
     */
    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Format distance for display.
     */
    public static String formatDistance(double distanceKm) {
        if (distanceKm < 1.0) {
            return String.format("%.0f m", distanceKm * 1000);
        }
        return String.format("%.1f km", distanceKm);
    }

    private synchronized void enforceRateLimit() throws InterruptedException {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;
        if (elapsed < 1100) { // 1.1 seconds to be safe
            Thread.sleep(1100 - elapsed);
        }
        lastRequestTime = System.currentTimeMillis();
    }
}
