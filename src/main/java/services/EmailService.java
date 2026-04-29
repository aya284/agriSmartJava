package services;

import entities.User;
import utils.ConfigLoader;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST  = "smtp.gmail.com";
    private static final String SMTP_PORT  = "587";
    private static final String FROM_EMAIL = ConfigLoader.get("EMAIL_USER");
    private static final String FROM_PASS  = ConfigLoader.get("EMAIL_PASS");

    // ── Shared session builder ─────────────────────────────────

    private Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASS);
            }
        });
    }

    // ── Password reset OTP ─────────────────────────────────────

    public void sendPasswordResetEmail(String toEmail,
                                        String otp) throws Exception {
        Session session = buildSession();
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL, "AgriSmart"));
        msg.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(toEmail));
        msg.setSubject("AgriSmart – Votre code de réinitialisation");
        msg.setContent(buildOtpHtml(otp), "text/html; charset=utf-8");
        Transport.send(msg);
    }

    // ── Status update (manual by admin) ───────────────────────

    public void sendStatusUpdateEmail(User user,
                                       String newStatus) throws Exception {
        Session session = buildSession();
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL, "AgriSmart"));
        msg.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(user.getEmail()));
        msg.setSubject("AgriSmart – Mise à jour de votre compte");
        msg.setContent(buildStatusUpdateHtml(user, newStatus),
                "text/html; charset=utf-8");
        Transport.send(msg);
    }

    // ── AI Verification result ─────────────────────────────────

    /**
     * Sends verification result email after automatic CIN analysis.
     *
     * @param user               the user
     * @param status             active / inactive / pending
     * @param reason             AI reason (can be null for pending)
     * @param transliteratedName Latin version of Arabic name (can be null)
     * @param confidence         AI confidence score (0 if pending)
     */
    public void sendVerificationResultEmail(User user,
                                             String status,
                                             String reason,
                                             String transliteratedName,
                                             int confidence) throws Exception {
        Session session = buildSession();
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL, "AgriSmart"));
        msg.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(user.getEmail()));

        String subject = switch (status.toLowerCase()) {
            case "active"   -> "AgriSmart – Votre compte est activé";
            case "inactive" -> "AgriSmart – Vérification de votre document";
            default         -> "AgriSmart – Votre dossier est en cours d'examen";
        };

        msg.setSubject(subject);
        msg.setContent(
            buildVerificationHtml(user, status, reason,
                    transliteratedName, confidence),
            "text/html; charset=utf-8"
        );
        Transport.send(msg);
        System.out.println("Verification email sent to: " + user.getEmail()
                + " [" + status + "]");
    }

    // ─────────────────────────────────────────────────────────
    //  HTML BUILDERS
    // ─────────────────────────────────────────────────────────

    private String buildVerificationHtml(User user, String status,
                                          String reason,
                                          String transliteratedName,
                                          int confidence) {
        String icon, color, title, message, extra;

        switch (status.toLowerCase()) {

            case "active" -> {
                icon    = "✅";
                color   = "#2ecc71";
                title   = "Votre compte a été activé !";
                message = "Votre carte CIN a été vérifiée avec succès. "
                        + "Vous pouvez maintenant vous connecter à AgriSmart.";
                extra   = ""; // No extra name info needed
            }

            case "inactive" -> {
                icon    = "❌";
                color   = "#e74c3c";
                title   = "Vérification échouée";
                message = "Nous n'avons pas pu valider votre carte CIN.";
                extra   = reason != null
                    ? "<div style='background:#fff5f5;border-left:4px solid #e74c3c;"
                    + "padding:12px 16px;border-radius:4px;margin:16px 0;'>"
                    + "<strong>Raison :</strong> " + reason + "</div>"
                    + "<p style='color:#555;'>Que faire :<br>"
                    + "• Vérifiez que votre CIN est valide et non expirée<br>"
                    + "• Prenez une photo claire et lisible<br>"
                    + "• Contactez le support si vous pensez qu'il s'agit d'une erreur</p>"
                    : "";
            }

            default -> {
                icon    = "⏳";
                color   = "#f1c40f";
                title   = "Votre dossier est en cours d'examen";
                message = "Notre système n'a pas pu prendre une décision automatique. "
                        + "Un administrateur va vérifier votre dossier manuellement. "
                        + "Ce processus prend généralement moins de 24 heures.";
                extra   = "";
            }
        }

        return """
            <div style="font-family:Arial,sans-serif;max-width:540px;margin:auto;
                        border:1px solid #e0e0e0;border-radius:12px;padding:40px;">
              <h2 style="color:#2a5438;margin-top:0;">AgriSmart</h2>
              <div style="border-bottom:2px solid #f0f0f0;margin:16px 0;"></div>
              <div style="text-align:center;font-size:48px;margin:20px 0;">%s</div>
              <h3 style="color:%s;text-align:center;font-size:20px;">%s</h3>
              <p style="color:#444;font-size:15px;">Bonjour %s,</p>
              <p style="color:#555;font-size:14px;">%s</p>
              %s
              <p style="color:#aaa;font-size:12px;margin-top:32px;border-top:1px solid #eee;
                         padding-top:16px;">
                Ceci est un message automatique — merci de ne pas y répondre.
              </p>
            </div>
        """.formatted(icon, color, title,
                user.getFirstName(), message, extra);
    }

    private String buildStatusUpdateHtml(User user, String status) {
        String statusColor = switch (status.toLowerCase()) {
            case "active"   -> "#2ecc71";
            case "inactive" -> "#e74c3c";
            case "pending"  -> "#f1c40f";
            default         -> "#34495e";
        };
        String statusLabel = switch (status.toLowerCase()) {
            case "active"   -> "Activé";
            case "inactive" -> "Désactivé";
            case "pending"  -> "En attente";
            default         -> status;
        };
        String message = switch (status.toLowerCase()) {
            case "active"   ->
                "Votre compte a été activé. Vous pouvez vous connecter.";
            case "inactive" ->
                "Votre compte a été désactivé. "
                + "Contactez le support si vous pensez qu'il s'agit d'une erreur.";
            case "pending"  ->
                "Votre compte est en attente de validation.";
            default -> "Le statut de votre compte a été mis à jour.";
        };

        return """
            <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;
                        border:1px solid #e0e0e0;border-radius:8px;padding:32px;">
              <h2 style="color:#2a5438;margin-top:0;">AgriSmart</h2>
              <h3>Mise à jour de votre compte</h3>
              <p>Bonjour %s,</p>
              <div style="background:#f4f6f8;border-radius:6px;padding:16px;
                          text-align:center;margin:20px 0;">
                <span style="color:%s;font-size:18px;font-weight:bold;
                             text-transform:uppercase;">%s</span>
              </div>
              <p style="color:#555;">%s</p>
              <p style="color:#888;font-size:13px;margin-top:24px;">
                Ceci est un message automatique.
              </p>
            </div>
        """.formatted(user.getFirstName(), statusColor, statusLabel, message);
    }

    private String buildOtpHtml(String otp) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;
                        border:1px solid #e0e0e0;border-radius:12px;
                        padding:40px;text-align:center;">
              <h2 style="color:#2a5438;margin-top:0;">AgriSmart</h2>
              <div style="border-bottom:2px solid #f0f0f0;margin:20px 0;"></div>
              <h3 style="color:#333;">Réinitialisation du mot de passe</h3>
              <p style="color:#555;">Utilisez ce code de vérification :</p>
              <div style="background:#f4f6f8;border-radius:12px;padding:20px;
                          font-family:monospace;font-size:32px;font-weight:bold;
                          color:#2a5438;letter-spacing:8px;margin:25px 0;">
                %s
              </div>
              <p style="color:#888;font-size:14px;">
                Valable pendant <strong>60 minutes</strong>.
              </p>
            </div>
        """.formatted(otp);
    }
}