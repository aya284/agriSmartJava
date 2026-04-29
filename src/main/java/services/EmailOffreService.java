package services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.activation.CommandMap;
import jakarta.activation.MailcapCommandMap;
import java.util.Properties;

public class EmailOffreService {

    private static final String SENDER_EMAIL = "akrem.zaied@etudiant-fsegt.utm.tn";
    private static final String APP_PASSWORD = "mvldkxpxzpvsqcik";
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    public static void sendCandidatureStatusEmail(String recipientEmail, String candidateName, String jobTitle, String location, String status) {

        // FIX : Pour éviter l'erreur de DataContentHandler
        Thread.currentThread().setContextClassLoader(EmailOffreService.class.getClassLoader());
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=org.eclipse.angus.mail.handlers.text_html");
        CommandMap.setDefaultCommandMap(mc);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "AgriSmart Recrutement"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));

            String subjectEmoji = "Acceptée".equalsIgnoreCase(status) ? "✅" : "❌";
            message.setSubject(subjectEmoji + " Mise à jour de votre candidature - AgriSmart");

            // --- CHANGEMENT ICI : On envoie du HTML ---
            String htmlContent = buildHtmlEmail(candidateName, jobTitle, location, status);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("[EmailOffreService] ✅ Email DESIGN envoyé !");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String buildHtmlEmail(String candidateName, String jobTitle, String location, String status) {
        boolean isAccepted = "Acceptée".equalsIgnoreCase(status);
        String color = isAccepted ? "#27ae60" : "#e74c3c";
        String statusText = isAccepted ? "✅ ACCEPTÉE" : "❌ REFUSÉE";

        return "<html><body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>" +
                "  <div style='max-width: 600px; margin: auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.1);'>" +
                "    <div style='background-color: #1a3323; padding: 20px; text-align: center; color: white;'>" +
                "      <h1 style='margin: 0;'>AgriSmart</h1>" +
                "      <p style='margin: 0; opacity: 0.8;'>Votre partenaire carrière agricole</p>" +
                "    </div>" +
                "    <div style='padding: 30px; color: #333;'>" +
                "      <p>Bonjour <strong>" + candidateName + "</strong>,</p>" +
                "      <p>Nous avons le plaisir de vous informer qu'une décision a été prise concernant votre candidature :</p>" +
                "      <div style='background: #f9f9f9; padding: 15px; border-left: 4px solid #1a3323; margin: 20px 0;'>" +
                "        <p style='margin: 5px 0;'><strong>Poste :</strong> " + jobTitle + "</p>" +
                "        <p style='margin: 5px 0;'><strong>Lieu :</strong> " + location + "</p>" +
                "      </div>" +
                "      <p style='text-align: center; margin-top: 30px;'>" +
                "        <span style='background-color: " + color + "; color: white; padding: 12px 25px; border-radius: 50px; font-weight: bold; font-size: 18px;'>" +
                "          " + statusText + "" +
                "        </span>" +
                "      </p>" +
                "    </div>" +
                "    <div style='background: #eee; padding: 15px; text-align: center; font-size: 12px; color: #777;'>" +
                "      Ceci est un message automatique, merci de ne pas y répondre.<br>© 2026 AgriSmart Tunisie" +
                "    </div>" +
                "  </div>" +
                "</body></html>";
    }
}