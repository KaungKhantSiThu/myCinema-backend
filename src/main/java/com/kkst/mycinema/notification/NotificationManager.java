package com.kkst.mycinema.notification;

import com.kkst.mycinema.entity.Booking;
import com.kkst.mycinema.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notification manager that coordinates sending notifications through various channels.
 * Uses the Observer pattern - multiple NotificationService implementations can handle notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationManager {

    private final List<NotificationService> notificationServices;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    /**
     * Send booking confirmation notification.
     */
    @Async
    public void sendBookingConfirmation(Booking booking) {
        log.info("Sending booking confirmation for booking: {}", booking.getId());

        Map<String, Object> data = buildBookingData(booking);
        data.put("confirmationNumber", String.format("CIN%06d", booking.getId()));

        var notification = Notification.builder()
                .type(NotificationType.EMAIL)
                .recipient(booking.getUser().getEmail())
                .recipientName(booking.getUser().getName())
                .subject("Your Booking Confirmation - " + booking.getShow().getMovie().getTitle())
                .templateName("booking-confirmation")
                .data(data)
                .priority(Notification.NotificationPriority.HIGH)
                .build();

        sendNotification(notification);
    }

    /**
     * Send booking cancellation notification.
     */
    @Async
    public void sendBookingCancellation(Booking booking) {
        log.info("Sending cancellation confirmation for booking: {}", booking.getId());

        Map<String, Object> data = buildBookingData(booking);
        data.put("refundAmount", booking.getTotalAmount());

        var notification = Notification.builder()
                .type(NotificationType.EMAIL)
                .recipient(booking.getUser().getEmail())
                .recipientName(booking.getUser().getName())
                .subject("Booking Cancelled - " + booking.getShow().getMovie().getTitle())
                .templateName("booking-cancellation")
                .data(data)
                .priority(Notification.NotificationPriority.NORMAL)
                .build();

        sendNotification(notification);
    }

    /**
     * Send show reminder (24 hours before).
     */
    @Async
    public void sendShowReminder(Booking booking) {
        log.info("Sending show reminder for booking: {}", booking.getId());

        Map<String, Object> data = buildBookingData(booking);

        var notification = Notification.builder()
                .type(NotificationType.EMAIL)
                .recipient(booking.getUser().getEmail())
                .recipientName(booking.getUser().getName())
                .subject("Reminder: Your show is tomorrow - " + booking.getShow().getMovie().getTitle())
                .templateName("show-reminder")
                .data(data)
                .priority(Notification.NotificationPriority.NORMAL)
                .build();

        sendNotification(notification);
    }

    /**
     * Send welcome email to new user.
     */
    @Async
    public void sendWelcomeEmail(User user) {
        log.info("Sending welcome email to: {}", user.getEmail());

        var notification = Notification.builder()
                .type(NotificationType.EMAIL)
                .recipient(user.getEmail())
                .recipientName(user.getName())
                .subject("Welcome to myCinema!")
                .templateName("welcome")
                .data(Map.of(
                        "userName", user.getName(),
                        "userEmail", user.getEmail()
                ))
                .priority(Notification.NotificationPriority.NORMAL)
                .build();

        sendNotification(notification);
    }

    /**
     * Send payment receipt.
     */
    @Async
    public void sendPaymentReceipt(Booking booking, String transactionId) {
        log.info("Sending payment receipt for booking: {}", booking.getId());

        Map<String, Object> data = buildBookingData(booking);
        data.put("transactionId", transactionId);
        data.put("paymentDate", booking.getBookingTime().format(DATE_FORMATTER));

        var notification = Notification.builder()
                .type(NotificationType.EMAIL)
                .recipient(booking.getUser().getEmail())
                .recipientName(booking.getUser().getName())
                .subject("Payment Receipt - myCinema")
                .templateName("payment-receipt")
                .data(data)
                .priority(Notification.NotificationPriority.NORMAL)
                .build();

        sendNotification(notification);
    }

    /**
     * Core method to dispatch notification to appropriate services.
     */
    private void sendNotification(Notification notification) {
        boolean sent = false;

        for (NotificationService service : notificationServices) {
            if (service.supports(notification.getType())) {
                try {
                    boolean success = service.send(notification);
                    if (success) {
                        sent = true;
                        log.debug("Notification sent successfully via {}", service.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    log.error("Failed to send notification via {}: {}",
                            service.getClass().getSimpleName(), e.getMessage());
                }
            }
        }

        if (!sent) {
            log.warn("No notification service could send the notification of type: {}",
                    notification.getType());
        }
    }

    /**
     * Build common booking data for templates.
     */
    private Map<String, Object> buildBookingData(Booking booking) {
        Map<String, Object> data = new HashMap<>();
        data.put("bookingId", booking.getId());
        data.put("userName", booking.getUser().getName());
        data.put("userEmail", booking.getUser().getEmail());
        data.put("movieTitle", booking.getShow().getMovie().getTitle());
        data.put("showDate", booking.getShow().getStartTime().format(DATE_FORMATTER));
        data.put("showTime", booking.getShow().getStartTime().format(TIME_FORMATTER));
        data.put("hallName", booking.getShow().getHall().getName());
        data.put("totalAmount", booking.getTotalAmount());
        data.put("seatCount", booking.getBookingSeats() != null ? booking.getBookingSeats().size() : 0);

        // Build seat list
        if (booking.getBookingSeats() != null) {
            var seatList = booking.getBookingSeats().stream()
                    .map(bs -> "Row " + bs.getShowSeat().getSeat().getRowNumber() +
                            ", Seat " + bs.getShowSeat().getSeat().getSeatNumber())
                    .toList();
            data.put("seats", seatList);
        }

        return data;
    }
}

