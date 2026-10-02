package com.skysentinel.security.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class OperatorNotificationService {
    private static final Logger log = LoggerFactory.getLogger(OperatorNotificationService.class);

    private final JavaMailSender mailSender;
    private final RestClient restClient;
    private final String fromEmail;
    private final String smtpHost;
    private final String smsWebhookUrl;

    public OperatorNotificationService(
            ObjectProvider<JavaMailSender> mailSender,
            RestClient.Builder restClientBuilder,
            @Value("${skysentinel.notifications.email.from:no-reply@skysentinel.local}") String fromEmail,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${skysentinel.notifications.sms.webhook-url:}") String smsWebhookUrl) {
        this.mailSender = mailSender.getIfAvailable();
        this.restClient = restClientBuilder.build();
        this.fromEmail = fromEmail;
        this.smtpHost = smtpHost;
        this.smsWebhookUrl = smsWebhookUrl;
    }

    public void sendRegistrationConfirmation(User user) {
        String subject = "SkySentinel account registration confirmed";
        String message = "Hello " + user.getFullName() + ",\n\n"
                + "Your SkySentinel Security operator account has been registered.\n"
                + "Badge: " + user.getBadgeNumber() + "\n\n"
                + "If you did not request this account, contact your SkySentinel administrator immediately.";
        sendEmail(user.getEmail(), subject, message);
    }

    public OtpDeliveryResult sendOtp(User user, String code) {
        String message = "Your SkySentinel verification code is " + code + ". It expires shortly and can only be used once.";
        boolean emailSent = sendEmail(user.getEmail(), "SkySentinel sign-in verification code", message);
        boolean smsSent = sendSms(user.getCellphoneNumber(), message);
        return new OtpDeliveryResult(emailSent, smsSent);
    }

    private boolean sendEmail(String to, String subject, String body) {
        if (mailSender == null || smtpHost == null || smtpHost.isBlank()) {
            log.warn("Email provider is not configured. Intended email to {} with subject: {}", to, subject);
            return false;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            return true;
        } catch (RuntimeException ex) {
            log.warn("Email delivery failed for {} with subject: {}", to, subject, ex);
            return false;
        }
    }

    private boolean sendSms(String to, String body) {
        if (smsWebhookUrl == null || smsWebhookUrl.isBlank()) {
            log.warn("SMS provider is not configured. Intended SMS to {}: {}", to, body);
            return false;
        }
        try {
            restClient.post()
                    .uri(smsWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("to", to, "message", body))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            log.warn("SMS delivery failed for {}", to, ex);
            return false;
        }
    }
}
