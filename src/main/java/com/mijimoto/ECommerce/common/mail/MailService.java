package com.mijimoto.ECommerce.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Simple mail sender service using MailHog.
 *
 * This service owns the content of emails (subjects / bodies / verification link).
 * UsersService should only call high-level methods here.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private final JavaMailSender mailSender;

    /**
     * Base URL used for verification link generation. If you have a frontend URL,
     * replace this value or make it configurable (e.g., via @Value from application.properties).
     */
    private static final String DEFAULT_VERIFICATION_BASE = "http://localhost:8080/api/v1/users/verify?code=";

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Generic send method (keeps a simple wrapper around JavaMailSender).
     */
    public void sendMail(String to, String subject, String text) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
            log.info("Mail sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw e;
        }
    }

    /**
     * Send verification email. Mail content + verification link are composed here (not in UsersService).
     *
     * @param to recipient email
     * @param code verification code (opaque)
     * @param ttlSeconds time to live (seconds) — included for user info in email
     */
    public void sendVerificationEmail(String to, String code, long ttlSeconds) {
        String subject = "Please verify your email";
        String link = DEFAULT_VERIFICATION_BASE + code;
        String body = new StringBuilder()
                .append("Hi,\n\n")
                .append("Thanks for creating an account. Please verify your email by clicking the link below:\n\n")
                .append(link)
                .append("\n\n")
                .append("This link is valid for approximately ")
                .append(ttlSeconds / 60)
                .append(" minutes.\n\n")
                .append("If you did not request this, you can ignore this email.\n\n")
                .append("Regards,\n")
                .append("Your Team")
                .toString();

        sendMail(to, subject, body);
    }

    /**
     * Optionally: add other mail methods here (reset password, notifications, etc.)
     */
}
