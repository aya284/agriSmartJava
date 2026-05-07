package utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Utility class to load application configurations from config.properties.
 * This ensures sensitive data like API keys are not hardcoded in the source.
 */
public class ConfigLoader {
    private static final String CONFIG_FILE = "config.properties";
    private static final Properties properties = new Properties();

    static {
        try {
            loadConfiguration();
            if (properties.isEmpty()) {
                System.err.println("CRITICAL: Unable to find " + CONFIG_FILE + " in the classpath or filesystem.");
            } else {
                System.out.println("ConfigLoader: Successfully loaded " + properties.size() + " properties.");
            }
        } catch (IOException ex) {
            System.err.println("ConfigLoader Error: Failed to load " + CONFIG_FILE + ": " + ex.getMessage());
        }
    }

    private static void loadConfiguration() throws IOException {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
                return;
            }
        }

        List<Path> candidates = List.of(
                Path.of(CONFIG_FILE),
                Path.of("src", "main", "resources", CONFIG_FILE),
                Path.of("config", CONFIG_FILE),
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

    /**
     * Gets a property value by key.
     * @param key The property key.
     * @return The property value or null if not found.
     */
    public static String get(String key) {
        return properties.getProperty(key);
    }

    /**
     * Gets a property value by key, with a default value.
     * @param key The property key.
     * @param defaultValue The default value if key is not found.
     * @return The property value or default.
     */
    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
