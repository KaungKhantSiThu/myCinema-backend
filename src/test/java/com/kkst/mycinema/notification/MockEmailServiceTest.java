package com.kkst.mycinema.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockEmailService Tests")
class MockEmailServiceTest {

    private MockEmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new MockEmailService();
    }

    @Test
    @DisplayName("Should send email notification successfully")
    void send_EmailNotification_Success() {
        // Given
        var notification = Notification.builder()
                .type(NotificationType.EMAIL)
                .recipient("test@example.com")
                .recipientName("Test User")
                .subject("Test Subject")
                .templateName("test-template")
                .data(Map.of("key", "value"))
                .priority(Notification.NotificationPriority.NORMAL)
                .build();

        // When
        boolean result = emailService.send(notification);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should not handle non-email notifications")
    void send_NonEmailNotification_ReturnsFalse() {
        // Given
        var notification = Notification.builder()
                .type(NotificationType.SMS)
                .recipient("+1234567890")
                .recipientName("Test User")
                .subject("Test")
                .templateName("sms-template")
                .priority(Notification.NotificationPriority.NORMAL)
                .build();

        // When
        boolean result = emailService.send(notification);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should support EMAIL notification type")
    void supports_EmailType_ReturnsTrue() {
        assertThat(emailService.supports(NotificationType.EMAIL)).isTrue();
    }

    @Test
    @DisplayName("Should not support SMS notification type")
    void supports_SmsType_ReturnsFalse() {
        assertThat(emailService.supports(NotificationType.SMS)).isFalse();
    }

    @Test
    @DisplayName("Should not support PUSH notification type")
    void supports_PushType_ReturnsFalse() {
        assertThat(emailService.supports(NotificationType.PUSH_NOTIFICATION)).isFalse();
    }
}

