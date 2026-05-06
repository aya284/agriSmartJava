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
        
        // Log configuration state for debugging
        logConfigurationState();
    }
    
    private void logConfigurationState() {
        System.out.println("=== PaymentGatewayService Configuration ===");
        System.out.println("Payment Mode: " + mode);
        System.out.println("Stripe Secret Key Configured: " + (!stripeSecretKey.isBlank()));
        System.out.println("Admin Email: " + (adminEmail.isBlank() ? "[not configured]" : adminEmail));
        System.out.println("Currency: " + stripeCurrency);
        
        if ("stripe".equals(mode) && stripeSecretKey.isBlank()) {
            System.err.println("WARNING: Stripe mode is enabled but STRIPE_SECRET_KEY is not configured!");
        }
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

        // Validate Stripe configuration
        if (stripeSecretKey.isBlank()) {
            String errorMsg = "Configuration Stripe manquante: STRIPE_SECRET_KEY n'est pas definie. " +
                    "Verifiez votre variable d'environnement ou propriete systeme.";
            System.err.println("PAYMENT ERROR: " + errorMsg);
            return new PaymentResult(false, "", errorMsg);
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
            System.err.println("PAYMENT ERROR: " + message);
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
            String stripeError = extractStripeError(body);
            
            // Handle authentication errors specifically
            if (statusCode == 401 || statusCode == 403) {
                String authErrorMsg = "Erreur d'authentification Stripe (HTTP " + statusCode + "): " +
                        "Verifiez que votre STRIPE_SECRET_KEY est correcte et valide. " +
                        "Detals: " + stripeError;
                System.err.println("STRIPE AUTH ERROR: " + authErrorMsg);
                return new PaymentResult(false, "", authErrorMsg);
            }
            
            // Handle invalid request format
            if (statusCode == 400) {
                String badRequestMsg = "Erreur de requete Stripe (HTTP 400): " + stripeError + 
                        ". Verifiez le format des donnees de paiement.";
                System.err.println("STRIPE BAD REQUEST: " + badRequestMsg);
                return new PaymentResult(false, "", badRequestMsg);
            }
            
            // Generic API error
            String genericMsg = "Erreur API Stripe (HTTP " + statusCode + "): " + stripeError;
            System.err.println("STRIPE API ERROR: " + genericMsg);
            return new PaymentResult(false, "", genericMsg);
        }

        if (body.isBlank()) {
            String msg = "Reponse Stripe vide.";
            System.err.println("PAYMENT ERROR: " + msg);
            return new PaymentResult(false, "", msg);
        }

        JSONObject json = new JSONObject(body);
        String status = json.optString("status", "").trim().toLowerCase(Locale.ROOT);
        String paymentIntentId = json.optString("id", "").trim();

        if ("succeeded".equals(status)) {
            System.out.println("Payment succeeded: " + paymentIntentId);
            return new PaymentResult(true, paymentIntentId, "");
        }

        if ("requires_action".equals(status) || "requires_source_action".equals(status)) {
            String msg = "Paiement requiert une verification 3D Secure (non supportee dans ce flux). Utilisez une carte test simple.";
            System.out.println("PAYMENT WARNING: " + msg);
            return new PaymentResult(false, "", msg);
        }

        if ("requires_payment_method".equals(status)) {
            String msg = "Paiement refuse. Verifiez la carte test utilisee.";
            System.out.println("PAYMENT INFO: " + msg);
            return new PaymentResult(false, "", msg);
        }

        String msg = "Paiement non confirme. Statut Stripe: " + status;
        System.out.println("PAYMENT WARNING: " + msg);
        return new PaymentResult(false, "", msg);
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
            String type = error.optString("type", "").trim();
            
            // Build descriptive error message
            StringBuilder errorDesc = new StringBuilder();
            if (!code.isBlank()) {
                errorDesc.append(code);
            }
            if (!type.isBlank()) {
                if (errorDesc.length() > 0) errorDesc.append(" [").append(type).append("]");
                else errorDesc.append(type);
            }
            
            if (errorDesc.length() > 0) {
                errorDesc.append(" - ");
            }
            errorDesc.append(message);
            
            return errorDesc.toString();
        } catch (Exception ex) {
            System.err.println("Failed to parse Stripe error: " + ex.getMessage());
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
    
    /**
     * Validates the Stripe configuration for the current mode.
     * Returns an empty string if valid, otherwise returns an error message.
     */
    public String validateConfiguration() {
        if ("mock".equals(mode)) {
            return ""; // Mock mode doesn't require credentials
        }
        
        if (!"stripe".equals(mode)) {
            return "Mode paiement invalide: " + mode;
        }
        
        if (stripeSecretKey.isBlank()) {
            return "Stripe mode is enabled but STRIPE_SECRET_KEY environment variable or system property " +
                    "marketplace.payment.stripe.secret is not configured. " +
                    "Set it before starting the application.";
        }
        
        if (!stripeSecretKey.startsWith("sk_test_") && !stripeSecretKey.startsWith("sk_live_")) {
            return "STRIPE_SECRET_KEY appears to be invalid. It should start with sk_test_ or sk_live_.";
        }
        
        return ""; // Configuration is valid
    }
    
    /**
     * Gets a human-readable description of the current payment configuration.
     */
    public String getConfigurationStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Payment Mode: ").append(mode).append("\n");
        
        if ("mock".equals(mode)) {
            status.append("Status: Mock mode - payments are simulated locally.\n");
        } else if ("stripe".equals(mode)) {
            if (stripeSecretKey.isBlank()) {
                status.append("Status: Stripe mode enabled but NOT CONFIGURED!\n");
                status.append("Required: Set STRIPE_SECRET_KEY environment variable or -Dmarketplace.payment.stripe.secret JVM property.\n");
            } else {
                String keyPreview = stripeSecretKey.substring(0, Math.min(10, stripeSecretKey.length())) + "...";
                status.append("Status: Stripe mode configured.\n");
                status.append("Secret Key: ").append(keyPreview).append("\n");
            }
        } else {
            status.append("Status: Unknown payment mode.\n");
        }
        
        status.append("Currency: ").append(stripeCurrency).append("\n");
        status.append("Admin Email: ").append(adminEmail.isBlank() ? "[not configured]" : adminEmail).append("\n");
        
        return status.toString();
    }
}