package com.kkst.mycinema.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * SMTP email service implementation for production email sending.
 *
 * Configuration required in application.properties:
 * - spring.mail.host
 * - spring.mail.port
 * - spring.mail.username
 * - spring.mail.password
 * - spring.mail.properties.mail.smtp.auth=true
 * - spring.mail.properties.mail.smtp.starttls.enable=true
 * - notification.email.from
 */
@Component
@ConditionalOnProperty(name = "notification.email.provider", havingValue = "smtp")
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements NotificationService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from:noreply@mycinema.com}")
    private String fromAddress;

    @Override
    public boolean send(Notification notification) {
        if (!supports(notification.getType())) {
            return false;
        }

        log.info("[SMTP] Sending email to: {}", notification.getRecipient());

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(notification.getRecipient());
            helper.setSubject(notification.getSubject());

            // Generate HTML content from template
            String htmlContent = generateHtmlContent(notification);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("[SMTP] Email sent successfully to: {}", notification.getRecipient());
            return true;

        } catch (Exception e) {
            log.error("[SMTP] Failed to send email to {}: {}", notification.getRecipient(), e.getMessage());
            return false;
        }
    }

    /**
     * Async method for sending emails without blocking the main thread
     */
    @Async
    public CompletableFuture<Boolean> sendAsync(Notification notification) {
        return CompletableFuture.completedFuture(send(notification));
    }

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.EMAIL;
    }

    /**
     * Generate HTML content from notification data.
     * In production, consider using a template engine like Thymeleaf or Freemarker.
     */
    private String generateHtmlContent(Notification notification) {
        Map<String, Object> data = notification.getData();
        String templateName = notification.getTemplateName();

        if ("booking-confirmation".equals(templateName)) {
            return generateBookingConfirmationEmail(data);
        } else if ("booking-cancellation".equals(templateName)) {
            return generateBookingCancellationEmail(data);
        }

        // Default template
        return generateDefaultEmail(notification.getSubject(), data);
    }

    private String generateBookingConfirmationEmail(Map<String, Object> data) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #1a1a2e; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background: #f9f9f9; }
                    .booking-details { background: white; padding: 15px; border-radius: 5px; margin: 15px 0; }
                    .booking-details h3 { margin-top: 0; color: #1a1a2e; }
                    .detail-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee; }
                    .total { font-size: 1.2em; font-weight: bold; color: #1a1a2e; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 0.9em; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>MyCinema</h1>
                        <h2>Booking Confirmation</h2>
                    </div>
                    <div class="content">
                        <p>Dear %s,</p>
                        <p>Thank you for your booking! Your seats have been confirmed.</p>
                        
                        <div class="booking-details">
                            <h3>Booking Details</h3>
                            <div class="detail-row">
                                <span>Booking ID:</span>
                                <span><strong>#%s</strong></span>
                            </div>
                            <div class="detail-row">
                                <span>Movie:</span>
                                <span><strong>%s</strong></span>
                            </div>
                            <div class="detail-row">
                                <span>Show Time:</span>
                                <span>%s</span>
                            </div>
                            <div class="detail-row">
                                <span>Hall:</span>
                                <span>%s</span>
                            </div>
                            <div class="detail-row">
                                <span>Seats:</span>
                                <span>%s</span>
                            </div>
                            <div class="detail-row total">
                                <span>Total Amount:</span>
                                <span>%s</span>
                            </div>
                        </div>
                        
                        <p><strong>Important:</strong> Please arrive at least 15 minutes before the show starts.</p>
                        <p>Show this confirmation email at the entrance.</p>
                    </div>
                    <div class="footer">
                        <p>Booked on: %s</p>
                        <p>© 2024 MyCinema. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                data.getOrDefault("userName", "Customer"),
                data.getOrDefault("bookingId", "N/A"),
                data.getOrDefault("movieTitle", "N/A"),
                data.getOrDefault("showTime", "N/A"),
                data.getOrDefault("hallName", "N/A"),
                data.getOrDefault("seats", "N/A"),
                data.getOrDefault("totalAmount", "N/A"),
                data.getOrDefault("bookingTime", "N/A")
            );
    }

    private String generateBookingCancellationEmail(Map<String, Object> data) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #dc3545; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background: #f9f9f9; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 0.9em; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>MyCinema</h1>
                        <h2>Booking Cancelled</h2>
                    </div>
                    <div class="content">
                        <p>Dear %s,</p>
                        <p>Your booking #%s has been cancelled.</p>
                        <p>If you paid for this booking, a refund will be processed within 5-7 business days.</p>
                        <p>We hope to see you again soon!</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 MyCinema. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                data.getOrDefault("userName", "Customer"),
                data.getOrDefault("bookingId", "N/A")
            );
    }

    private String generateDefaultEmail(String subject, Map<String, Object> data) {
        StringBuilder content = new StringBuilder();
        content.append("<html><body>");
        content.append("<h2>").append(subject).append("</h2>");
        content.append("<ul>");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            content.append("<li><strong>").append(entry.getKey()).append(":</strong> ")
                   .append(entry.getValue()).append("</li>");
        }
        content.append("</ul>");
        content.append("</body></html>");
        return content.toString();
    }
}

