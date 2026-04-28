package services;

import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PaymentGatewayService {

    public record CardInput(String cardNumber, String expMonth, String expYear, String cvc, String holderName) {}

    public record PaymentResult(boolean success, String paymentReference, String errorMessage) {}

    private static final DateTimeFormatter REF_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String STRIPE_PAYMENT_INTENT_URL = "https://api.stripe.com/v1/payment_intents";

    private final HttpClient httpClient;
    private final String mode;
    private final String stripeSecretKey;
    private final String adminEmail;
    private final String stripeCurrency;

    public PaymentGatewayService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mode = readConfig("marketplace.payment.mode", "PAYMENT_API_MODE", "mock").trim().toLowerCase(Locale.ROOT);
        this.stripeSecretKey = readConfig("marketplace.payment.stripe.secret", "STRIPE_SECRET_KEY", "").trim();
        this.adminEmail = readConfig("marketplace.payment.admin.email", "ADMIN_EMAIL", "").trim();
        this.stripeCurrency = readConfig("marketplace.payment.currency", "PAYMENT_API_CURRENCY", "usd")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    public PaymentResult chargeCard(double amount, int customerId, String description, CardInput cardInput) {
        if (amount <= 0) {
            return new PaymentResult(false, "", "Montant de paiement invalide.");
        }

        if ("mock".equals(mode)) {
            return new PaymentResult(true, buildMockReference(customerId), "");
        }

        if (!"stripe".equals(mode)) {
            return new PaymentResult(false, "", "Mode paiement inconnu: " + mode + ". Utilisez mock ou stripe.");
        }

        if (stripeSecretKey.isBlank()) {
            return new PaymentResult(false, "", "STRIPE_SECRET_KEY manquante.");
        }

        String paymentMethod = resolveStripeTestPaymentMethod(cardInput == null ? "" : cardInput.cardNumber());
        try {
            long amountInMinor = Math.max(1L, Math.round(amount * 100.0));

            String form = buildStripeForm(amountInMinor, paymentMethod, customerId, description);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(STRIPE_PAYMENT_INTENT_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + stripeSecretKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseStripeResponse(response);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return new PaymentResult(false, "", "Erreur technique paiement: " + message);
        }
    }

    private String buildStripeForm(long amountInMinor, String paymentMethod, int customerId, String description) {
        StringBuilder form = new StringBuilder();
        appendForm(form, "amount", String.valueOf(amountInMinor));
        appendForm(form, "currency", stripeCurrency);
        appendForm(form, "confirm", "true");
        appendForm(form, "payment_method", paymentMethod);
        appendForm(form, "description", description == null ? "Commande marketplace" : description);
        appendForm(form, "metadata[customer_id]", String.valueOf(customerId));
        appendForm(form, "metadata[integration]", "desktop_in_app");

        if (!adminEmail.isBlank()) {
            appendForm(form, "receipt_email", adminEmail);
        }

        return form.toString();
    }

    private void appendForm(StringBuilder target, String key, String value) {
        if (target.length() > 0) {
            target.append('&');
        }
        target.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
        target.append('=');
        target.append(URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8));
    }

    private PaymentResult parseStripeResponse(HttpResponse<String> response) {
        String body = response.body() == null ? "" : response.body();
        int statusCode = response.statusCode();

        if (statusCode >= 400) {
            return new PaymentResult(false, "", "Erreur Stripe HTTP " + statusCode + ": " + extractStripeError(body));
        }

        if (body.isBlank()) {
            return new PaymentResult(false, "", "Reponse Stripe vide.");
        }

        JSONObject json = new JSONObject(body);
        String status = json.optString("status", "").trim().toLowerCase(Locale.ROOT);
        String paymentIntentId = json.optString("id", "").trim();

        if ("succeeded".equals(status)) {
            return new PaymentResult(true, paymentIntentId, "");
        }

        if ("requires_action".equals(status) || "requires_source_action".equals(status)) {
            return new PaymentResult(false, "", "Paiement requiert une verification 3D Secure (non supportee dans ce flux). Utilisez une carte test simple.");
        }

        if ("requires_payment_method".equals(status)) {
            return new PaymentResult(false, "", "Paiement refuse. Verifiez la carte test utilisee.");
        }

        return new PaymentResult(false, "", "Paiement non confirme. Statut Stripe: " + status);
    }

    private String extractStripeError(String body) {
        if (body == null || body.isBlank()) {
            return "Erreur inconnue.";
        }
        try {
            JSONObject json = new JSONObject(body);
            JSONObject error = json.optJSONObject("error");
            if (error == null) {
                return body;
            }
            String code = error.optString("code", "").trim();
            String message = error.optString("message", "Erreur Stripe.").trim();
            if (!code.isBlank()) {
                return code + " - " + message;
            }
            return message;
        } catch (Exception ex) {
            return body;
        }
    }

    private String buildMockReference(int customerId) {
        return "MOCK-" + customerId + "-" + REF_TIME.format(LocalDateTime.now());
    }

    private String resolveStripeTestPaymentMethod(String cardNumber) {
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("\\D", "");
        if ("4000000000009995".equals(digits)) {
            return "pm_card_chargeCustomerFail";
        }
        if ("4000002500003155".equals(digits)) {
            return "pm_card_authenticationRequired";
        }
        if ("4000000000003220".equals(digits)) {
            return "pm_card_chargeDeclinedInsufficientFunds";
        }
        return "pm_card_visa";
    }

    private String readConfig(String systemProperty, String envKey, String defaultValue) {
        String fromSystem = System.getProperty(systemProperty);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem;
        }
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return defaultValue;
    }
}