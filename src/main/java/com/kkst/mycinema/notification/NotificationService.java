package com.kkst.mycinema.notification;

/**
 * Observer interface for notification events.
 * Implementations can send emails, SMS, push notifications, etc.
 */
public interface NotificationService {

    /**
     * Send a notification
     * @param notification the notification to send
     * @return true if sent successfully
     */
    boolean send(Notification notification);

    /**
     * Check if this service can handle the notification type
     */
    boolean supports(NotificationType type);
}

