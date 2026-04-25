package services;

import utils.ConfigLoader;
import fi.iki.elonen.NanoHTTPD;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class RecaptchaService extends NanoHTTPD {

    public static final String SITE_KEY    = ConfigLoader.get("RECAPTCHA_SITE_KEY");
    private static final String SECRET_KEY = ConfigLoader.get("RECAPTCHA_SECRET_KEY");
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public static final int PORT = 9090;

    // ─── NanoHTTPD Server ────────────────────────────────────────────────────

    private static RecaptchaService instance;

    public RecaptchaService() {
        super(PORT);
    }

    public void startServer() {
        try {
            if (!isAlive()) {
                start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
                System.out.println("reCAPTCHA local server started on http://localhost:" + PORT);
            }
        } catch (IOException e) {
            System.err.println("reCAPTCHA server already running or port " + PORT + " is busy.");
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        return newFixedLengthResponse(
                Response.Status.OK,
                "text/html",
                getRecaptchaHtml()
        );
    }

    public void stopServer() {
        if (isAlive()) {
            stop();
            System.out.println("reCAPTCHA server stopped.");
        }
    }

    // ─── Verify Token ────────────────────────────────────────────────────────

    public boolean verify(String recaptchaResponse) {
        if (recaptchaResponse == null || recaptchaResponse.isBlank()) {
            return false;
        }

        try {
            URL url = new URL(VERIFY_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            String params = "secret=" + SECRET_KEY + "&response=" + recaptchaResponse;
            byte[] postData = params.getBytes(StandardCharsets.UTF_8);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JSONObject json = new JSONObject(response.toString());
            return json.optBoolean("success", false);

        } catch (Exception e) {
            System.err.println("reCAPTCHA verification failed: " + e.getMessage());
            return false;
        }
    }

    // ─── HTML Page ───────────────────────────────────────────────────────────

    public static String getRecaptchaHtml() {
        if (SITE_KEY == null || SITE_KEY.isBlank()) {
            return """
                <!DOCTYPE html>
                <html>
                <body style="background:transparent;color:red;font-family:sans-serif;font-size:12px;text-align:center;">
                    <p>⚠ Erreur reCAPTCHA : Clé du site manquante</p>
                </body>
                </html>
                """;
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            margin: 0;
                            padding: 0;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                            background: #f8f9fa;
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        }
                        .container {
                            display: flex;
                            flex-direction: column;
                            align-items: center;
                            gap: 15px;
                        }
                    </style>
                    <script src="https://www.google.com/recaptcha/api.js" async defer></script>
                    <script>
                        function onCaptchaSuccess(token) {
                            if (window.javaConnector) {
                                window.javaConnector.onCaptchaCompleted(token);
                            }
                        }
                        function onCaptchaExpired() {
                            if (window.javaConnector) {
                                window.javaConnector.onCaptchaExpired();
                            }
                        }
                    </script>
                </head>
                <body>
                    <div class="container">
                        <div class="g-recaptcha"
                             data-sitekey="%s"
                             data-callback="onCaptchaSuccess"
                             data-expired-callback="onCaptchaExpired">
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(SITE_KEY);
    }
}