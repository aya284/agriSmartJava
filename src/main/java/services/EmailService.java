package services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    // ── Gmail SMTP config ─────────────────────────────────────

    private static final String SMTP_HOST  = "smtp.gmail.com";
    private static final String SMTP_PORT  = "587";
    private static final String FROM_EMAIL = "abirbenkhlifa17@gmail.com";
    private static final String FROM_PASS  = "rwqh foyv dpio bjku";

    public void sendPasswordResetEmail(String toEmail, String fullToken) throws Exception {
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
        msg.setSubject("AgriSmart – Réinitialisation du mot de passe");
        msg.setContent(buildHtml(fullToken), "text/html; charset=utf-8");

        Transport.send(msg);
    }

    private String buildHtml(String token) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;
                        border:1px solid #e0e0e0;border-radius:8px;padding:32px;">
              <h2 style="color:#2ecc71;margin-top:0;">AgriSmart</h2>
              <h3>Réinitialisation du mot de passe</h3>
              <p>Vous avez demandé à réinitialiser votre mot de passe.<br>
                 Copiez le jeton ci-dessous et collez-le dans l'application :</p>
              <div style="background:#f4f6f8;border-radius:6px;padding:16px;
                          font-family:monospace;font-size:14px;
                          word-break:break-all;letter-spacing:0.5px;">
                %s
              </div>
              <p style="color:#888;font-size:13px;margin-top:24px;">
                Ce jeton expire dans <strong>60 minutes</strong>.<br>
                Si vous n'êtes pas à l'origine de cette demande, ignorez cet e-mail.
              </p>
            </div>
        """.formatted(token);
    }
}
