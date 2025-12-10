package com.kkst.mycinema.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity to track seat holds during checkout process.
 * Seats are held temporarily (default 10 min) while user completes payment.
 */
@Entity
@Table(name = "seat_holds", indexes = {
        @Index(name = "idx_seat_hold_show", columnList = "show_id"),
        @Index(name = "idx_seat_hold_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique token to identify the hold (used in API)
     */
    @Column(name = "hold_token", nullable = false, unique = true, length = 64)
    private String holdToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private HoldStatus status = HoldStatus.ACTIVE;

    /**
     * Comma-separated list of show_seat IDs being held
     */
    @Column(name = "seat_ids", nullable = false, length = 1000)
    private String seatIds;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return status == HoldStatus.ACTIVE && !isExpired();
    }

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    public enum HoldStatus {
        ACTIVE, // Hold is active
        PAYMENT_PENDING, // Payment in progress (locked)
        CONFIRMED, // Converted to booking
        RELEASED, // Manually released by user
        EXPIRED // Automatically expired
    }
}
