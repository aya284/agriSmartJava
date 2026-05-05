package services;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Uses <a href="https://www.exchangerate-api.com/">ExchangeRate-API</a> standard endpoint.
 * API key: {@code EXCHANGE_RATE_API_KEY} in {@code local.secrets.properties}.
 */
public class ExchangeRateService {

    private static final String LOCAL_SECRETS_FILE = "local.secrets.properties";
    private static final String PROP_KEY = "EXCHANGE_RATE_API_KEY";
    private static final String API_URL = "https://v6.exchangerate-api.com/v6/%s/latest/%s";

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration CACHE_TTL = Duration.ofMinutes(45);

    private volatile Snapshot cache;
    private volatile Instant cacheExpiry = Instant.EPOCH;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    public Optional<Snapshot> getSnapshot(boolean forceRefresh) {
        if (!forceRefresh && cache != null && Instant.now().isBefore(cacheExpiry)) {
            return Optional.of(cache);
        }
        Optional<Snapshot> fetched = fetchRemote();
        if (fetched.isPresent()) {
            cache = fetched.get();
            cacheExpiry = Instant.now().plus(CACHE_TTL);
            return Optional.of(cache);
        }
        if (cache != null) {
            return Optional.of(cache);
        }
        return Optional.empty();
    }

    public boolean hasApiKey() {
        return !readApiKey().isBlank();
    }

    /**
     * Human-readable line for the marketplace header (French-style decimals when locale is FR).
     */
    public String formatSnapshot(Snapshot s, Locale locale) {
        String eur = String.format(locale, "%.3f", s.eurPerTnd());
        String usd = String.format(locale, "%.3f", s.usdPerTnd());
        String when = DateTimeFormatter.ofPattern("dd/MM HH:mm")
                .withZone(ZoneOffset.UTC)
                .format(s.lastUpdateUtc());
        return "1 TND ≈ " + eur + " EUR · " + usd + " USD   •   MAJ UTC " + when;
    }

    public String missingKeyMessage() {
        return "Ajoutez EXCHANGE_RATE_API_KEY dans local.secrets.properties pour afficher les taux.";
    }

    public String unavailableMessage() {
        return "Taux de change indisponibles pour le moment.";
    }

    private Optional<Snapshot> fetchRemote() {
        String apiKey = readApiKey();
        if (apiKey.isBlank()) {
            return Optional.empty();
        }

        Optional<Snapshot> tndBase = requestLatest(apiKey, "TND");
        if (tndBase.isPresent()) {
            return tndBase;
        }

        return requestUsdDerived(apiKey);
    }

    private Optional<Snapshot> requestLatest(String apiKey, String baseCode) {
        try {
            String body = httpGet(String.format(API_URL, apiKey, baseCode));
            JSONObject root = new JSONObject(body);
            if (!"success".equals(root.optString("result"))) {
                return Optional.empty();
            }
            JSONObject rates = root.optJSONObject("conversion_rates");
            if (rates == null) {
                return Optional.empty();
            }
            Instant update = Instant.ofEpochSecond(root.optLong("time_last_update_unix", 0));

            if ("TND".equalsIgnoreCase(baseCode)) {
                double eur = rates.optDouble("EUR", Double.NaN);
                double usd = rates.optDouble("USD", Double.NaN);
                if (!Double.isFinite(eur) || !Double.isFinite(usd)) {
                    return Optional.empty();
                }
                return Optional.of(new Snapshot(eur, usd, update));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Base USD: rates are "currency per 1 USD". Derive EUR and USD per 1 TND.
     */
    private Optional<Snapshot> requestUsdDerived(String apiKey) {
        try {
            String body = httpGet(String.format(API_URL, apiKey, "USD"));
            JSONObject root = new JSONObject(body);
            if (!"success".equals(root.optString("result"))) {
                return Optional.empty();
            }
            JSONObject rates = root.optJSONObject("conversion_rates");
            if (rates == null) {
                return Optional.empty();
            }
            double tndPerUsd = rates.optDouble("TND", Double.NaN);
            double eurPerUsd = rates.optDouble("EUR", Double.NaN);
            if (!Double.isFinite(tndPerUsd) || tndPerUsd <= 0 || !Double.isFinite(eurPerUsd)) {
                return Optional.empty();
            }
            double usdPerTnd = 1.0 / tndPerUsd;
            double eurPerTnd = eurPerUsd / tndPerUsd;
            Instant update = Instant.ofEpochSecond(root.optLong("time_last_update_unix", 0));
            return Optional.of(new Snapshot(eurPerTnd, usdPerTnd, update));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String httpGet(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + resp.statusCode());
        }
        return resp.body();
    }

    private String readApiKey() {
        Properties props = loadLocalSecrets();
        return props.getProperty(PROP_KEY, "").trim();
    }

    private Properties loadLocalSecrets() {
        Properties properties = new Properties();
        java.util.List<Path> candidates = java.util.List.of(
                Path.of(LOCAL_SECRETS_FILE),
                Path.of("config", LOCAL_SECRETS_FILE),
                Path.of("src", "main", "resources", LOCAL_SECRETS_FILE)
        );
        for (Path candidate : candidates) {
            if (!Files.exists(candidate) || !Files.isRegularFile(candidate)) {
                continue;
            }
            try (var stream = Files.newInputStream(candidate)) {
                properties.load(stream);
                break;
            } catch (IOException ignored) {
            }
        }
        return properties;
    }

    public record Snapshot(double eurPerTnd, double usdPerTnd, Instant lastUpdateUtc) {}
}
