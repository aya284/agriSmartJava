package services;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class NewsService {
    private static final String API_KEY = "e86da1000a82480aa79058e482fb1c8c";

    public static List<Article> getLatestAgricultureNews() {
        List<Article> newsList = new ArrayList<>();

        // FILTRE : Emploi, Recrutement et Salaires dans l'agriculture
        // On utilise l'encodage URL pour les espaces (%20) et les guillemets (%22)
        String query = "(%22recrutement%20agricole%22%20OR%20%22emploi%20agricole%22%20OR%20%22salaire%20agriculture%22)";
        String finalUrl = "https://newsapi.org/v2/everything?q=agriculture%20emploi&language=fr&apiKey=" + API_KEY;

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(finalUrl))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                if (jsonResponse.has("articles")) {
                    JSONArray articles = jsonResponse.getJSONArray("articles");
                    for (int i = 0; i < Math.min(articles.length(), 20); i++) {
                        JSONObject obj = articles.getJSONObject(i);
                        newsList.add(new Article(
                                obj.optString("title", "Sans titre"),
                                obj.optString("url", "#"),
                                obj.optString("urlToImage", "")
                        ));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newsList;
    }

    public static class Article {
        public String title;
        public String url;
        public String imageUrl;

        public Article(String title, String url, String imageUrl) {
            this.title = title;
            this.url = url;
            this.imageUrl = imageUrl;
        }
    }
}