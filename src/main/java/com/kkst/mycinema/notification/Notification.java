package com.kkst.mycinema.notification;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Notification data object containing all information needed to send a notification.
 */
@Getter
@Builder
public class Notification {
    private final NotificationType type;
    private final String recipient;           // Email address, phone number, etc.
    private final String recipientName;
    private final String subject;
    private final String templateName;        // Template to use for rendering
    private final Map<String, Object> data;   // Template variables
    private final NotificationPriority priority;
    private final LocalDateTime scheduledAt;  // null for immediate sending

    public enum NotificationPriority {
        LOW, NORMAL, HIGH, URGENT
    }

    /**
     * Check if this notification should be sent immediately.
     */
    public boolean isImmediate() {
        return scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now());
    }
}

