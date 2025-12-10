package com.kkst.mycinema.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "show_seats", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "show_id", "seat_id" })
}, indexes = {
        @Index(name = "idx_show_seat_status", columnList = "show_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // CRITICAL: Optimistic Locking for concurrency control
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    // Seat hold/lock fields
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "locked_by_user_id")
    private Long lockedByUserId;

    /**
     * Check if this seat is currently locked (held by another user)
     */
    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    /**
     * Check if this seat is available for booking or holding
     */
    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE && !isLocked();
    }

    /**
     * Check if this seat is locked by a specific user
     */
    public boolean isLockedByUser(Long userId) {
        return isLocked() && lockedByUserId != null && lockedByUserId.equals(userId);
    }

    /**
     * Lock the seat for a user until a specified time
     */
    public void lockForUser(Long userId, LocalDateTime until) {
        this.lockedByUserId = userId;
        this.lockedUntil = until;
        this.status = SeatStatus.LOCKED;
    }

    /**
     * Release the lock on this seat
     */
    public void releaseLock() {
        this.lockedByUserId = null;
        this.lockedUntil = null;
        this.status = SeatStatus.AVAILABLE;
    }

    public enum SeatStatus {
        AVAILABLE, BOOKED, LOCKED
    }
}
