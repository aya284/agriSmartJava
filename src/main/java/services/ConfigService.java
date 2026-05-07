package services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public class ConfigService {
    private static Properties properties = new Properties();

    static {
        try {
            loadConfiguration();
        } catch (Exception e) {
            System.err.println("ERREUR : Impossible de charger le fichier config.properties : " + e.getMessage());
        }
    }

    private static void loadConfiguration() throws IOException {
        try (InputStream input = ConfigService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
                return;
            }
        }

        List<Path> candidates = List.of(
                Path.of("config.properties"),
                Path.of("src", "main", "resources", "config.properties"),
                Path.of("config", "config.properties"),
                Path.of("local.secrets.properties"),
                Path.of("config", "local.secrets.properties"),
                Path.of("src", "main", "resources", "local.secrets.properties")
        );

        for (Path candidate : candidates) {
            if (!Files.exists(candidate) || !Files.isRegularFile(candidate)) {
                continue;
            }
            try (InputStream input = Files.newInputStream(candidate)) {
                properties.load(input);
                return;
            }
        }
    }

    public static String getHuggingFaceKey() {
        return properties.getProperty("huggingface.key", "");
    }

    public static String getGeminiKey() {
        return properties.getProperty("gemini.key", "");
    }

    public static String getPlantIdKey() {
        return properties.getProperty("plantid.key", "");
    }

    public static String getGoogleCalendarKey() {
        return properties.getProperty("GOOGLE_CALENDAR_API_KEY", "");
    }
}
