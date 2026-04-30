package services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import entities.Task;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.util.List;
import java.util.Properties;

public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "AgriSmart";
    private static final String API_KEY = loadGoogleApiKey();
    private static final String CALENDAR_ID = "dd8f31c87f25f66a8fb8d1386f0f23cd80d9d595231528715f2a6ca04551a0b9@group.calendar.google.com";
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/calendar");

    private static String loadGoogleApiKey() {
        // 1. Try environment variable first
        String envKey = System.getenv("GOOGLE_CALENDAR_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }

        // 2. Fallback to config.properties
        try (InputStream input = GoogleCalendarService.class.getResourceAsStream("/config.properties")) {
            if (input == null) {
                System.err.println("WARNING: config.properties not found for Google Calendar.");
            } else {
                Properties prop = new Properties();
                prop.load(input);
                String propKey = prop.getProperty("GOOGLE_CALENDAR_API_KEY");
                if (propKey != null && !propKey.trim().isEmpty() && !propKey.contains("your_")) {
                    return propKey.trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading Google API key from config.properties: " + e.getMessage());
        }

        System.err.println("WARNING: GOOGLE_CALENDAR_API_KEY not found in environment or config.properties.");
        return "";
    }

    private Calendar getCalendarService() throws Exception {
        var transport = GoogleNetHttpTransport.newTrustedTransport();
        var jsonFactory = GsonFactory.getDefaultInstance();

        // 1. Try Service Account (Best for Group Calendars)
        InputStream serviceAccountIn = getClass().getResourceAsStream("/service_account.json");
        if (serviceAccountIn != null) {
            com.google.auth.oauth2.GoogleCredentials credentials = com.google.auth.oauth2.ServiceAccountCredentials.fromStream(serviceAccountIn)
                    .createScoped(SCOPES);
            return new Calendar.Builder(transport, jsonFactory, new com.google.api.client.http.HttpRequestInitializer() {
                @Override
                public void initialize(com.google.api.client.http.HttpRequest request) throws java.io.IOException {
                    try {
                        credentials.refreshIfExpired();
                        request.getHeaders().setAuthorization("Bearer " + credentials.getAccessToken().getTokenValue());
                    } catch (Exception e) {
                        throw new java.io.IOException(e);
                    }
                }
            }).setApplicationName(APPLICATION_NAME).build();
        }

        // 2. Try OAuth2 (Requires Browser Login)
        InputStream in = getClass().getResourceAsStream("/client_secret.json");
        Credential cred = null;
        if (in != null) {
            var secrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in));
            var flow = new GoogleAuthorizationCodeFlow.Builder(
                    transport, jsonFactory, secrets, SCOPES)
                    .setAccessType("online")
                    .build();
            var receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            cred = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }

        return new Calendar.Builder(transport, jsonFactory, cred)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public void addTaskToCalendar(Task task) throws Exception {
        Calendar service = getCalendarService();

        Event event = new Event()
                .setSummary(task.getTitre())
                .setDescription(task.getDescription())
                .setLocation(task.getLocalisation());

        DateTime startDateTime = new DateTime(task.getDateDebut().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone(ZoneId.systemDefault().getId());
        event.setStart(start);

        DateTime endDateTime = new DateTime(task.getDateFin() != null 
                ? task.getDateFin().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : task.getDateDebut().plusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone(ZoneId.systemDefault().getId());
        event.setEnd(end);

        service.events().insert(CALENDAR_ID, event).setKey(API_KEY).execute();
    }

    public List<Event> getEvents(long timeMin, long timeMax) throws Exception {
        Calendar service = getCalendarService();
        return service.events().list(CALENDAR_ID)
                .setKey(API_KEY)
                .setTimeMin(new DateTime(timeMin))
                .setTimeMax(new DateTime(timeMax))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()
                .getItems();
    }
    public String generateGoogleCalendarUrl(Task task) {
        try {
            String baseUrl = "https://www.google.com/calendar/render?action=TEMPLATE";
            String text = "&text=" + java.net.URLEncoder.encode(task.getTitre(), "UTF-8");
            String details = "&details=" + java.net.URLEncoder.encode(task.getDescription() != null ? task.getDescription() : "", "UTF-8");
            String location = "&location=" + java.net.URLEncoder.encode(task.getLocalisation() != null ? task.getLocalisation() : "", "UTF-8");
            
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
            String start = task.getDateDebut().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).format(formatter);
            String end = (task.getDateFin() != null ? task.getDateFin() : task.getDateDebut().plusHours(1))
                    .atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).format(formatter);
            String dates = "&dates=" + start + "/" + end;
            
            String add = "&add=" + java.net.URLEncoder.encode(CALENDAR_ID, "UTF-8");
            
            return baseUrl + text + details + location + dates + add;
        } catch (Exception e) {
            return null;
        }
    }
}
