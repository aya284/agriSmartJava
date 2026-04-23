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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class GoogleAuthService {

    private static final List<String> SCOPES = List.of(
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile"
    );

    public Userinfo authenticate() throws Exception {
        var transport   = GoogleNetHttpTransport.newTrustedTransport();
        var jsonFactory = GsonFactory.getDefaultInstance();

        // Load credentials from resources
        InputStream in = getClass().getResourceAsStream("/client_secret.json");
        if (in == null) {
            throw new Exception("Fichier client_secret.json introuvable dans les resources.");
        }
        var secrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in));

        // Build flow — no DataStore so no token caching, always prompts login
        var flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, jsonFactory, secrets, SCOPES)
                .setAccessType("online")
                .build();

        // Use a random available port so it never conflicts
        var receiver = new LocalServerReceiver.Builder()
                .setHost("localhost")
                .setPort(8888)
                .build();

        Credential cred = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        return new Oauth2.Builder(transport, jsonFactory, cred)
                .setApplicationName("AgriSmart")
                .build()
                .userinfo().get().execute();
    }
}