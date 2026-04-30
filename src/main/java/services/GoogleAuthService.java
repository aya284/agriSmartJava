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
            "https://www.googleapis.com/auth/userinfo.profile",
            "https://www.googleapis.com/auth/calendar"
    );

    public Userinfo authenticate() throws Exception {
        var transport   = GoogleNetHttpTransport.newTrustedTransport();
        var jsonFactory = GsonFactory.getDefaultInstance();

        InputStream in = getClass().getResourceAsStream("/client_secret.json");
        var secrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in));

        var flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, jsonFactory, secrets, SCOPES)
                .setAccessType("online")
                .build();

        var receiver   = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential cred = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        return new Oauth2.Builder(transport, jsonFactory, cred)
                .setApplicationName("AgriSmart")
                .build()
                .userinfo().get().execute();
    }
}