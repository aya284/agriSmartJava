package services;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service for Google reCAPTCHA v2 verification.
 * Handles server-side token validation against Google's siteverify endpoint.
 */
public class RecaptchaService {

    public static final String SITE_KEY   = "6LfmO3AsAAAAAKO_wR99qojsk3hXCPVStn-EfjoB";
    private static final String SECRET_KEY = "6LfmO3AsAAAAAPqpMlyQfq6QcaQOFW92u3-lvXH0";
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    /**
     * Verifies a reCAPTCHA response token with Google's server.
     *
     * @param recaptchaResponse the g-recaptcha-response token from the client
     * @return true if the token is valid, false otherwise
     */
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

    /**
     * Returns the HTML page that renders the reCAPTCHA v2 checkbox widget.
     * The page uses a JavaScript callback to signal the JavaFX WebView
     * when the user completes the captcha.
     */
    public static String getRecaptchaHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            margin: 0;
                            padding: 8px 0;
                            display: flex;
                            justify-content: center;
                            align-items: flex-start;
                            background: transparent;
                            overflow: hidden;
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
                    <div class="g-recaptcha"
                         data-sitekey="%s"
                         data-callback="onCaptchaSuccess"
                         data-expired-callback="onCaptchaExpired">
                    </div>
                </body>
                </html>
                """.formatted(SITE_KEY);
    }
}
