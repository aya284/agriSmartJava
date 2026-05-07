package services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GoogleAuthService {

    private static final List<String> SCOPES = List.of(
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile",
            "https://www.googleapis.com/auth/calendar"
    );

    public Userinfo authenticate() throws Exception {
        var transport   = GoogleNetHttpTransport.newTrustedTransport();
        var jsonFactory = GsonFactory.getDefaultInstance();

                // Load credentials from classpath first, then filesystem fallback.
                InputStream in = openClientSecretStream();
        if (in == null) {
            throw new Exception("Fichier client_secret.json introuvable dans les resources.");
        }
        var secrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in));

        // Build flow — no DataStore so no token caching, always prompts login
        var flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, jsonFactory, secrets, SCOPES)
                .setAccessType("online")
                .build();

        // Use -1 to let the OS assign a random available port
        var receiver = new LocalServerReceiver.Builder()
                .setHost("localhost")
                .setPort(-1)
                .build();

        Credential cred = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        return new Oauth2.Builder(transport, jsonFactory, cred)
                .setApplicationName("AgriSmart")
                .build()
                .userinfo().get().execute();
    }

        private InputStream openClientSecretStream() throws IOException {
                InputStream classpathStream = getClass().getResourceAsStream("/client_secret.json");
                if (classpathStream != null) {
                        return classpathStream;
                }

                List<Path> candidates = List.of(
                                Path.of("client_secret.json"),
                                Path.of("src", "main", "resources", "client_secret.json"),
                                Path.of("src", "main", "client_secret.json")
                );

                for (Path candidate : candidates) {
                        if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                                return Files.newInputStream(candidate);
                        }
                }

                return null;
        }
}