package services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * SMS notification service using Twilio REST API.
 * Sends SMS alerts for stock events via HTTP POST (no SDK needed).
 *
 * Requires a Twilio account: https://www.twilio.com
 * Trial accounts can send to verified phone numbers only.
 */
public class SmsService {

    // ── Twilio config (replace with your credentials) ──────────
    // You can also load these from a properties file
    private String accountSid = "";
    private String authToken = "";
    private String fromNumber = ""; // Twilio phone number, e.g. "+1234567890"

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private boolean configured = false;

    public SmsService() {
        loadConfig();
    }

    /**
     * Try to load Twilio credentials from system properties or config.
     */
    private void loadConfig() {
        try {
            java.util.Properties props = new java.util.Properties();
            var stream = getClass().getResourceAsStream("/local.secrets.properties");
            if (stream != null) {
                props.load(stream);
                stream.close();
            }
            String sid = props.getProperty("twilio.account.sid", System.getProperty("twilio.account.sid", ""));
            String token = props.getProperty("twilio.auth.token", System.getProperty("twilio.auth.token", ""));
            String from = props.getProperty("twilio.from.number", System.getProperty("twilio.from.number", ""));

            if (!sid.isEmpty() && !token.isEmpty() && !from.isEmpty()) {
                this.accountSid = sid;
                this.authToken = token;
                this.fromNumber = from;
                this.configured = true;
            }
        } catch (Exception e) {
            System.err.println("SMS config not loaded: " + e.getMessage());
        }
    }

    /**
     * Check if SMS service is properly configured with Twilio credentials.
     */
    public boolean isConfigured() {
        return configured;
    }

    /**
     * Send an SMS message via Twilio REST API.
     *
     * @param toNumber recipient phone number in E.164 format (e.g. "+21612345678")
     * @param message  the SMS body text
     * @return true if sent successfully
     */
    public boolean sendSms(String toNumber, String message) {
        if (!configured) {
            System.out.println("[SMS] Not configured — skipping SMS to " + toNumber);
            System.out.println("[SMS] Message: " + message);
            return false;
        }

        if (toNumber == null || toNumber.trim().isEmpty()) {
            return false;
        }

        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            String formData = "To=" + URLEncoder.encode(toNumber.trim(), StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(fromNumber, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

            String auth = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                System.out.println("[SMS] Sent successfully to " + toNumber);
                return true;
            } else {
                System.err.println("[SMS] Failed (" + response.statusCode() + "): " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("[SMS] Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send a stock alert SMS to a seller.
     */
    public boolean sendStockAlertSms(String sellerPhone, String productName, int remainingStock) {
        String msg = "AgriSmart: Stock faible pour \"" + productName + "\" — "
                + remainingStock + " unite(s) restante(s). Pensez a reapprovisionner !";
        return sendSms(sellerPhone, msg);
    }

    /**
     * Send a restock notification SMS to a wishlist user.
     */
    public boolean sendRestockSms(String userPhone, String productName) {
        String msg = "AgriSmart: Bonne nouvelle ! \"" + productName
                + "\" est de nouveau en stock sur le marketplace.";
        return sendSms(userPhone, msg);
    }
}
