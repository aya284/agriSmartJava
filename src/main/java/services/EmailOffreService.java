package services;

// CHANGEMENT ICI : On utilise jakarta au lieu de javax
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

/**
 * Service dédié à l'envoi d'emails professionnels pour AgriSmart.
 */
public class EmailOffreService {

    private static final String SENDER_EMAIL = "akrem.zaied@etudiant-fsegt.utm.tn";
    private static final String APP_PASSWORD = "mvldkxpxzpvsqcik";
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    public static void sendCandidatureStatusEmail(String recipientEmail, String candidateName, String jobTitle, String location, String status) {

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        // On précise jakarta.mail.Authenticator pour être sûr
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
            message.setSubject("🔔 Mise à jour de votre candidature - " + status);

            String statusColor;
            String statusIcon;

            // Nettoyage simple pour le switch
            String checkStatus = (status == null) ? "" : status.toLowerCase().trim();

            switch (checkStatus) {
                case "acceptée":
                    statusColor = "#28a745";
                    statusIcon = "✅";
                    break;
                case "refusée":
                    statusColor = "#dc3545";
                    statusIcon = "❌";
                    break;
                default:
                    statusColor = "#ffc107";
                    statusIcon = "⏳";
                    break;
            }

            String htmlBody =
                    "<html>" +
                            "<body style='margin: 0; padding: 0; font-family: Segoe UI, Arial, sans-serif; background-color: #f4f7f6;'>" +
                            "  <div style='max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1);'>" +
                            "    <div style='background: linear-gradient(135deg, #1a4331 0%, #2d5a3d 100%); color: white; padding: 30px; text-align: center;'>" +
                            "      <h1 style='margin: 0; font-size: 24px;'>AgriSmart</h1>" +
                            "      <p style='margin: 5px 0 0; opacity: 0.8;'>Votre partenaire carrière agricole</p>" +
                            "    </div>" +
                            "    <div style='padding: 30px; color: #444;'>" +
                            "      <p style='font-size: 16px;'>Bonjour <strong>" + candidateName + "</strong>,</p>" +
                            "      <p>Nous avons le plaisir de vous informer qu'une décision a été prise concernant votre candidature :</p>" +
                            "      <div style='background-color: #f8f9fa; border-left: 5px solid #1a4331; padding: 20px; margin: 25px 0; border-radius: 4px;'>" +
                            "        <p style='margin: 0 0 10px 0;'><strong>Poste :</strong> " + jobTitle + "</p>" +
                            "        <p style='margin: 0;'><strong>Lieu :</strong> " + location + "</p>" +
                            "      </div>" +
                            "      <div style='text-align: center; margin-top: 35px;'>" +
                            "        <p style='font-size: 12px; color: #888; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 10px;'>Nouveau Statut</p>" +
                            "        <div style='display: inline-block; background-color: " + statusColor + "; color: white; padding: 12px 40px; border-radius: 50px; font-weight: bold; font-size: 18px; box-shadow: 0 4px 10px rgba(0,0,0,0.1);'>" +
                            statusIcon + " " + (status != null ? status.toUpperCase() : "") +
                            "        </div>" +
                            "      </div>" +
                            "    </div>" +
                            "    <div style='background-color: #f1f1f1; padding: 20px; text-align: center; font-size: 12px; color: #777;'>" +
                            "      Ceci est un message automatique, merci de ne pas y répondre.<br>" +
                            "      © 2026 <strong>AgriSmart Tunisie</strong>" +
                            "    </div>" +
                            "  </div>" +
                            "</body>" +
                            "</html>";

            message.setContent(htmlBody, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("DEBUG: Email envoyé avec succès !");

        } catch (Exception e) {
            System.err.println("ERREUR EMAIL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}