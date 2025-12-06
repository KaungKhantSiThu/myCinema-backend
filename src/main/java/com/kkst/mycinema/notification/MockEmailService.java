package com.kkst.mycinema.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock email service for development and testing.
 * Logs emails instead of actually sending them.
 *
 * Replace with SmtpEmailService or SendGridEmailService in production.
 */
@Component
@ConditionalOnProperty(name = "notification.email.provider", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class MockEmailService implements NotificationService {

    @Override
    public boolean send(Notification notification) {
        if (!supports(notification.getType())) {
            return false;
        }

        log.info("========================================");
        log.info("[MOCK EMAIL] Sending email notification");
        log.info("To: {} <{}>", notification.getRecipientName(), notification.getRecipient());
        log.info("Subject: {}", notification.getSubject());
        log.info("Template: {}", notification.getTemplateName());
        log.info("Priority: {}", notification.getPriority());
        log.info("Data: {}", notification.getData());
        log.info("========================================");

        // Simulate sending delay
        simulateNetworkDelay();

        log.info("[MOCK EMAIL] Email sent successfully to {}", notification.getRecipient());
        return true;
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.EMAIL;
    }

    private void simulateNetworkDelay() {
        try {
            Thread.sleep(50 + (long) (Math.random() * 100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

