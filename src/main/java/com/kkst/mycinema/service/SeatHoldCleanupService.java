package com.kkst.mycinema.service;

import com.kkst.mycinema.entity.ShowSeat;
import com.kkst.mycinema.repository.SeatHoldRepository;
import com.kkst.mycinema.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Service responsible for cleaning up expired seat holds.
 * Runs periodically to release seats that were held but never confirmed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatHoldCleanupService {

    private final SeatHoldRepository seatHoldRepository;
    private final ShowSeatRepository showSeatRepository;

    /**
     * Scheduled job to clean up expired seat holds.
     * Runs every minute by default (configurable via booking.seat-hold.cleanup-cron).
     */
    @Scheduled(cron = "${booking.seat-hold.cleanup-cron:0 * * * * *}")
    @Transactional
    public void cleanupExpiredHolds() {
        var now = LocalDateTime.now();
        log.debug("Running seat hold cleanup job at {}", now);

        // 1. Find all expired holds that are still marked as ACTIVE
        var expiredHolds = seatHoldRepository.findExpiredHolds(now);

        if (expiredHolds.isEmpty()) {
            log.debug("No expired holds to clean up");
            return;
        }

        log.info("Found {} expired seat holds to clean up", expiredHolds.size());

        int releasedCount = 0;

        // 2. Process each expired hold
        for (var hold : expiredHolds) {
            try {
                // Parse seat IDs
                var seatIds = Arrays.stream(hold.getSeatIds().split(","))
                        .map(Long::parseLong)
                        .toList();

                // Get the seats
                var showSeats = showSeatRepository.findByShowIdAndIdIn(hold.getShow().getId(), seatIds);

                // Release only seats that are still locked by this hold's user
                for (var seat : showSeats) {
                    if (seat.isLockedByUser(hold.getUser().getId())) {
                        seat.releaseLock();
                        releasedCount++;
                    }
                }
                showSeatRepository.saveAll(showSeats);

                // Mark hold as expired
                hold.setStatus(com.kkst.mycinema.entity.SeatHold.HoldStatus.EXPIRED);
                seatHoldRepository.save(hold);

                log.debug("Released hold {} with {} seats", hold.getHoldToken(), seatIds.size());

            } catch (Exception e) {
                log.error("Error cleaning up hold {}: {}", hold.getHoldToken(), e.getMessage());
            }
        }

        log.info("Seat hold cleanup completed. Released {} seats from {} holds",
                releasedCount, expiredHolds.size());
    }

    /**
     * Manual trigger for cleanup (can be called from admin endpoint).
     */
    @Transactional
    public int manualCleanup() {
        log.info("Manual seat hold cleanup triggered");

        // Use bulk update for efficiency
        int updated = seatHoldRepository.markExpiredHolds(LocalDateTime.now());

        // Release locked seats
        cleanupExpiredHolds();

        return updated;
    }
}

