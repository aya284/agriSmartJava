package services;

import utils.ConfigLoader;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    // ── Gmail SMTP config ─────────────────────────────────────

    private static final String SMTP_HOST  = "smtp.gmail.com";
    private static final String SMTP_PORT  = "587";
    private static final String FROM_EMAIL = ConfigLoader.get("EMAIL_USER");
    private static final String FROM_PASS  = ConfigLoader.get("EMAIL_PASS");

    public void sendPasswordResetEmail(String toEmail, String otp) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASS);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL, "AgriSmart"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject("AgriSmart – Votre code de réinitialisation");
        msg.setContent(buildHtml(otp), "text/html; charset=utf-8");

        Transport.send(msg);
    }

    public void sendStatusUpdateEmail(entities.User user, String newStatus) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASS);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL, "AgriSmart"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user.getEmail()));
        msg.setSubject("AgriSmart – Mise à jour de votre compte");
        msg.setContent(buildStatusUpdateHtml(user, newStatus), "text/html; charset=utf-8");

        Transport.send(msg);
    }

    private String buildStatusUpdateHtml(entities.User user, String status) {
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
            case "active"   -> "Votre compte a été activé. Vous pouvez maintenant vous connecter et profiter de toutes les fonctionnalités d'AgriSmart.";
            case "inactive" -> "Votre compte a été désactivé. Si vous pensez qu'il s'agit d'une erreur, veuillez contacter notre support.";
            case "pending"  -> "Votre compte est actuellement en attente de validation par nos administrateurs.";
            default         -> "Le statut de votre compte a été mis à jour.";
        };

        return """
            <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;
                        border:1px solid #e0e0e0;border-radius:8px;padding:32px;">
              <h2 style="color:#2ecc71;margin-top:0;">AgriSmart</h2>
              <h3>Mise à jour de votre compte</h3>
              <p>Bonjour %s,</p>
              <p>Nous vous informons que l'administrateur a modifié le statut de votre profil :</p>
              <div style="background:#f4f6f8;border-radius:6px;padding:16px;
                          text-align:center;margin:20px 0;">
                <span style="color:%s;font-size:18px;font-weight:bold;text-transform:uppercase;">
                  %s
                </span>
              </div>
              <p>%s</p>
              <p style="color:#888;font-size:13px;margin-top:24px;">
                Ceci est un message automatique, merci de ne pas y répondre directement.
              </p>
            </div>
        """.formatted(user.getFirstName(), statusColor, statusLabel, message);
    }

    private String buildHtml(String otp) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;
                        border:1px solid #e0e0e0;border-radius:12px;padding:40px;text-align:center;">
              <h2 style="color:#2a5438;margin-top:0;font-size:28px;">AgriSmart</h2>
              <div style="border-bottom: 2px solid #f0f0f0; margin: 20px 0;"></div>
              <h3 style="color:#333;font-size:20px;">Réinitialisation du mot de passe</h3>
              <p style="color:#555;font-size:16px;">Vous avez demandé à réinitialiser votre mot de passe.<br>
                 Utilisez le code de vérification ci-dessous :</p>
              <div style="background:#f4f6f8;border-radius:12px;padding:20px;
                          font-family:monospace;font-size:32px;font-weight:bold;
                          color:#2a5438;letter-spacing:8px;margin:25px 0;">
                %s
              </div>
              <p style="color:#888;font-size:14px;margin-top:30px;">
                Ce code est valable pendant <strong>60 minutes</strong>.<br>
                Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet e-mail.
              </p>
            </div>
        """.formatted(otp);
    }
}