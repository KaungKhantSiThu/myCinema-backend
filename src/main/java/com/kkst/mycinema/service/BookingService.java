package com.kkst.mycinema.service;

import com.kkst.mycinema.config.MetricsConfig;
import com.kkst.mycinema.dto.BookingRequest;
import com.kkst.mycinema.dto.BookingResponse;
import com.kkst.mycinema.dto.CancellationResponse;
import com.kkst.mycinema.dto.PaymentConfirmationRequest;
import com.kkst.mycinema.dto.SeatHoldResponse;
import com.kkst.mycinema.entity.*;
import com.kkst.mycinema.exception.*;
import com.kkst.mycinema.notification.NotificationManager;
import com.kkst.mycinema.payment.PaymentService;
import com.kkst.mycinema.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final MetricsConfig metricsConfig;
    private final PaymentService paymentService;
    private final NotificationManager notificationManager;

    @Value("${booking.seat-hold.duration-minutes:10}")
    private int seatHoldDurationMinutes;

    // =====================================================
    // SEAT HOLD METHODS (New Feature)
    // =====================================================

    /**
     * Hold seats temporarily during checkout process.
     * Seats are locked for a configurable duration (default 10 minutes).
     */
    @Transactional
    public SeatHoldResponse holdSeats(BookingRequest request, String userEmail) {
        log.info("Holding seats for user: {} with {} seats", userEmail, request.seatIds().size());

        // 1. Validate show exists and is in the future
        var show = showRepository.findById(request.showId())
                .orElseThrow(() -> new ShowNotFoundException(request.showId()));

        if (show.getStartTime().isBefore(LocalDateTime.now())) {
            throw new InvalidBookingException("Cannot hold seats for past shows");
        }

        // 2. Get user
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        // 3. Fetch seats
        var showSeats = showSeatRepository.findByShowIdAndIdIn(request.showId(), request.seatIds());

        // 4. Validate all requested seats exist
        if (showSeats.size() != request.seatIds().size()) {
            throw new InvalidBookingException("One or more seats do not exist or do not belong to this show");
        }

        // 5. Check if all seats are available (not booked and not locked by others)
        var unavailableSeats = showSeats.stream()
                .filter(seat -> !seat.isAvailable() && !seat.isLockedByUser(user.getId()))
                .toList();

        if (!unavailableSeats.isEmpty()) {
            throw new SeatUnavailableException();
        }

        // 6. Calculate expiration time
        var expiresAt = LocalDateTime.now().plusMinutes(seatHoldDurationMinutes);

        // 7. Lock the seats
        for (var showSeat : showSeats) {
            showSeat.lockForUser(user.getId(), expiresAt);
        }
        showSeatRepository.saveAll(showSeats);

        // 8. Create hold record
        var holdToken = UUID.randomUUID().toString();
        var seatIdsCsv = request.seatIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        var seatHold = SeatHold.builder()
                .holdToken(holdToken)
                .user(user)
                .show(show)
                .seatIds(seatIdsCsv)
                .expiresAt(expiresAt)
                .status(SeatHold.HoldStatus.ACTIVE)
                .build();
        seatHoldRepository.save(seatHold);

        log.info("Seats held successfully. Token: {}, Expires: {}", holdToken, expiresAt);

        // 9. Build response
        var seatInfos = showSeats.stream()
                .map(ss -> SeatHoldResponse.SeatInfo.builder()
                        .seatId(ss.getId())
                        .rowNumber(ss.getSeat().getRowNumber())
                        .seatNumber(ss.getSeat().getSeatNumber())
                        .price(ss.getPrice())
                        .build())
                .toList();

        return SeatHoldResponse.builder()
                .holdToken(holdToken)
                .showId(show.getId())
                .movieTitle(show.getMovie().getTitle())
                .showTime(show.getStartTime())
                .heldSeats(seatInfos)
                .holdExpiresAt(expiresAt)
                .message("Seats held for " + seatHoldDurationMinutes + " minutes. Complete payment to confirm.")
                .build();
    }

    /**
     * Confirm a seat hold and create the booking.
     * 
     * @deprecated Use confirmHoldWithPayment for production to ensure payment is
     *             processed
     */
    @Transactional
    public BookingResponse confirmHold(String holdToken, String userEmail) {
        return confirmHoldInternal(holdToken, userEmail, null);
    }

    /**
     * Confirm a seat hold with payment processing.
     * This is the production-ready method that processes payment before confirming
     * booking.
     * Refactored to handle transaction boundaries correctly.
     */
    public BookingResponse confirmHoldWithPayment(PaymentConfirmationRequest request, String userEmail) {
        log.info("Confirming hold with payment: {} for user: {}", request.holdToken(), userEmail);

        // 1. Initiate Payment (Lock the hold) - Transactional
        // Returns the hold ID and calculated amount to ensure we work with consistent
        // data
        var initializationResult = initiatePayment(request.holdToken(), userEmail);
        var holdId = initializationResult.holdId();
        var amount = initializationResult.amount();

        // 2. Process Payment - NON-Transactional
        // Database connection is released here while we wait for payment gateway
        String transactionId;
        try {
            log.info("Processing payment for hold: {}", request.holdToken());
            // We need to fetch user/show again or pass minimal info.
            // For safety, we can re-fetch or use data from initialization if we passed it
            // back.
            // But since we are outside transaction, entities from initiatePayment might be
            // detached.
            // Let's rely on the amount we calculated safely in step 1.

            // To construct the Booking object for payment service (if it needs User/Show
            // entities),
            // we might need to re-fetch them lightly or accept that they are detached.
            // Assuming PaymentService just needs amount and metadata.
            // If it needs attached entities, we might need a read-only transaction or
            // similar.
            // Looking at previous code: paymentService.processBookingPayment(booking,
            // method)
            // It constructed a temporary booking object.

            // Let's carry over necessary data in the record
            var bookingForPayment = Booking.builder()
                    .user(initializationResult.user())
                    .show(initializationResult.show())
                    .totalAmount(amount)
                    .build();

            var paymentResult = paymentService.processBookingPayment(bookingForPayment, request.holdToken(),
                    request.paymentMethod());
            transactionId = paymentResult.getTransactionId();
            log.info("Payment successful. Transaction ID: {}", transactionId);

        } catch (Exception e) {
            log.error("Payment processing failed or error occurred: {}", e.getMessage());
            // 2b. Revert Hold Status - Transactional
            revertHoldStatus(holdId);
            throw e;
        }

        // 3. Complete Booking - Transactional
        try {
            return completeBooking(holdId, transactionId);
        } catch (OptimisticLockingFailureException | SeatHoldExpiredException e) {
            // CRITICAL: Payment succeeded but Booking failed.
            // This is the "Ghost Booking" scenario.
            // We MUST refund or alert.
            log.error("CRITICAL: Payment succeeded but Booking failed for Hold ID: {}. Transaction: {}", holdId,
                    transactionId);

            // AUTOMATIC REFUND IMPLEMENTATION
            try {
                log.info("Initiating automatic refund for transaction: {}", transactionId);
                paymentService.processRefund(transactionId, amount);
                log.info("Automatic refund successful for transaction: {}", transactionId);
            } catch (Exception refundEx) {
                log.error("FATAL: Automatic refund failed for transaction: {}. Manual intervention required!",
                        transactionId, refundEx);
                // In a real system, would trigger PagerDuty/Alert here
            }

            throw new BookingFailedAfterPaymentException(
                    "Booking failed after successful payment. Automatic refund has been initiated. Transaction Ref: "
                            + transactionId,
                    e);
        }
    }

    /**
     * Step 1 Validation & Locking
     */
    @Transactional
    public PaymentInitResult initiatePayment(String holdToken, String userEmail) {
        // 1. Find the hold
        var seatHold = seatHoldRepository.findByHoldToken(holdToken)
                .orElseThrow(() -> new SeatHoldExpiredException(holdToken, true));

        // 2. Validate ownership
        if (!seatHold.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedBookingAccessException("This hold does not belong to you");
        }

        // 3. Check if hold is active
        if (seatHold.getStatus() != SeatHold.HoldStatus.ACTIVE) {
            throw new SeatHoldExpiredException("Hold is not active (Status: " + seatHold.getStatus() + ")");
        }

        if (seatHold.isExpired()) {
            seatHold.setStatus(SeatHold.HoldStatus.EXPIRED);
            seatHoldRepository.save(seatHold);
            throw new SeatHoldExpiredException("Hold has expired");
        }

        // 4. Update status to PENDING to prevent concurrent double-pay
        seatHold.setStatus(SeatHold.HoldStatus.PAYMENT_PENDING);
        // Optimistic locking (@Version) will trigger here if someone else beat us
        seatHold = seatHoldRepository.save(seatHold);

        // 5. Calculate amount safely inside this Tx
        var seatIds = Arrays.stream(seatHold.getSeatIds().split(","))
                .map(Long::parseLong)
                .toList();

        var showSeats = showSeatRepository.findByShowIdAndIdIn(seatHold.getShow().getId(), seatIds);
        var totalAmount = showSeats.stream()
                .map(ShowSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PaymentInitResult(seatHold.getId(), totalAmount, seatHold.getUser(), seatHold.getShow());
    }

    /**
     * Step 2b Revert
     */
    @Transactional
    public void revertHoldStatus(Long holdId) {
        try {
            var seatHold = seatHoldRepository.findById(holdId).orElse(null);
            if (seatHold != null && seatHold.getStatus() == SeatHold.HoldStatus.PAYMENT_PENDING) {
                seatHold.setStatus(SeatHold.HoldStatus.ACTIVE);
                seatHoldRepository.save(seatHold);
                log.info("Reverted hold {} status to ACTIVE after payment failure", holdId);
            }
        } catch (Exception e) {
            log.error("Failed to revert hold status for ID {}", holdId, e);
        }
    }

    /**
     * Step 3 Completion
     */
    @Transactional
    public BookingResponse completeBooking(Long holdId, String transactionId) {
        // 1. Fetch hold again
        var seatHold = seatHoldRepository.findById(holdId)
                .orElseThrow(() -> new SeatHoldExpiredException("Hold not found during completion"));

        // 2. Double check status
        if (seatHold.getStatus() != SeatHold.HoldStatus.PAYMENT_PENDING) {
            throw new InvalidBookingException("Hold is not in PENDING state. Current: " + seatHold.getStatus());
        }

        // 3. Parse seat IDs
        var seatIds = Arrays.stream(seatHold.getSeatIds().split(","))
                .map(Long::parseLong)
                .toList();

        // 4. Get the seats
        var showSeats = showSeatRepository.findByShowIdAndIdIn(seatHold.getShow().getId(), seatIds);

        // 5. Verify seats are still locked by this user (Redundant but safe)
        for (var seat : showSeats) {
            if (!seat.isLockedByUser(seatHold.getUser().getId())) {
                throw new SeatUnavailableException("One or more seats are no longer held");
            }
        }

        // 6. Create booking
        // totalAmount is recalculated or trusted from previous step?
        // Safer to recalculate or trust the Hold entity if we stored it (we didn't
        // store value in Hold).
        // Let's recalculate to be 100% sure we book what we record.
        var totalAmount = showSeats.stream()
                .map(ShowSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var booking = Booking.builder()
                .user(seatHold.getUser())
                .show(seatHold.getShow())
                .bookingTime(LocalDateTime.now())
                .status(Booking.BookingStatus.CONFIRMED)
                .totalAmount(totalAmount)
                .transactionId(transactionId)
                .build();
        booking = bookingRepository.save(booking);

        // 7. Update seats to BOOKED and clear lock
        for (var showSeat : showSeats) {
            showSeat.setStatus(ShowSeat.SeatStatus.BOOKED);
            showSeat.setLockedUntil(null);
            showSeat.setLockedByUserId(null);
        }
        showSeatRepository.saveAll(showSeats);

        // 8. Create junction records
        var bookingSeats = new ArrayList<BookingSeat>();
        for (var showSeat : showSeats) {
            var bookingSeat = BookingSeat.builder()
                    .booking(booking)
                    .showSeat(showSeat)
                    .build();
            bookingSeats.add(bookingSeat);
        }
        bookingSeatRepository.saveAll(bookingSeats);

        // 9. Mark hold as confirmed
        seatHold.setStatus(SeatHold.HoldStatus.CONFIRMED);
        seatHoldRepository.save(seatHold);

        log.info("Booking confirmed. Booking ID: {}", booking.getId());
        metricsConfig.getBookingSuccessCounter().increment();

        sendBookingConfirmationEmail(booking);

        return mapToBookingResponse(booking, showSeats);
    }

    /**
     * DTO for internal state passing.
     */
    public record PaymentInitResult(Long holdId, BigDecimal amount, User user, Show show) {
    }

    /**
     * Internal method to confirm hold with optional payment processing
     * Replaced by split logic. Retained only for deprecated confirmHold if needed,
     * but since confirmHold is deprecated, we will minimalize it or remove logic.
     * To support the deprecated generic confirmHold (no payment), we can keep a
     * simplified version.
     */
    private BookingResponse confirmHoldInternal(String holdToken, String userEmail,
            PaymentConfirmationRequest paymentRequest) {
        // Because we heavily refactored, the old logic is gone.
        // If confirmHold (deprecated) calls this, we should support it ONLY if payment
        // is null.
        // But confirmHoldWithPayment no longer calls this.
        // So this is only for the deprecated no-payment method.
        if (paymentRequest != null) {
            throw new IllegalStateException("Use confirmHoldWithPayment for payments");
        }
        // Reuse the complete logic but fake the PENDING state transition?
        // For backward compatibility, let's keep the old monolithic transaction JUST
        // for the deprecated method
        // Or better, just implement logical flow.
        // Simpler: Just do the old logic for the deprecated method.

        // 1. Find and validation ...
        // ... (Logic copied from original is too long).
        // Let's just implement it simply as "inituate + complete" without payment step.

        var init = initiatePayment(holdToken, userEmail);
        return completeBooking(init.holdId(), null);
    }

    /**
     * Send booking confirmation email to user
     */
    private void sendBookingConfirmationEmail(Booking booking) {
        try {
            // Use NotificationManager which handles email sending asynchronously
            notificationManager.sendBookingConfirmation(booking);
            log.info("Booking confirmation email queued for: {}", booking.getUser().getEmail());
        } catch (Exception e) {
            // Log but don't fail the booking
            log.error("Failed to send booking confirmation email: {}", e.getMessage());
        }
    }

    /**
     * Release a seat hold (user cancelled checkout).
     */
    @Transactional
    public void releaseHold(String holdToken, String userEmail) {
        log.info("Releasing hold: {} for user: {}", holdToken, userEmail);

        // 1. Find the hold
        var seatHold = seatHoldRepository.findByHoldToken(holdToken)
                .orElseThrow(() -> new SeatHoldExpiredException(holdToken, true));

        // 2. Validate ownership
        if (!seatHold.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedBookingAccessException("This hold does not belong to you");
        }

        // 3. Only release if active
        if (seatHold.getStatus() == SeatHold.HoldStatus.ACTIVE) {
            // 4. Parse seat IDs and release locks
            var seatIds = Arrays.stream(seatHold.getSeatIds().split(","))
                    .map(Long::parseLong)
                    .toList();

            var showSeats = showSeatRepository.findByShowIdAndIdIn(seatHold.getShow().getId(), seatIds);
            for (var seat : showSeats) {
                if (seat.isLockedByUser(seatHold.getUser().getId())) {
                    seat.releaseLock();
                }
            }
            showSeatRepository.saveAll(showSeats);

            // 5. Mark hold as released
            seatHold.setStatus(SeatHold.HoldStatus.RELEASED);
            seatHoldRepository.save(seatHold);

            log.info("Hold released successfully: {}", holdToken);
        }
    }

    // =====================================================
    // DIRECT BOOKING METHODS (Original)
    // =====================================================

    /**
     * CRITICAL METHOD: Books seats with optimistic locking to prevent double
     * booking
     * This is the core of the concurrency control mechanism
     */
    @Transactional
    public BookingResponse bookSeats(BookingRequest request, String userEmail) {
        return metricsConfig.getBookingDurationTimer().record(() -> {
            try {
                log.info("Starting booking process for user: {} with {} seats",
                        userEmail, request.seatIds().size());

                // 1. Validate show exists and is in the future
                var show = showRepository.findById(request.showId())
                        .orElseThrow(() -> new ShowNotFoundException(request.showId()));

                if (show.getStartTime().isBefore(LocalDateTime.now())) {
                    throw new InvalidBookingException("Cannot book seats for past shows");
                }

                // 2. Get user
                var user = userRepository.findByEmail(userEmail)
                        .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

                // 3. Fetch seats with optimistic lock
                var showSeats = showSeatRepository.findByShowIdAndIdIn(
                        request.showId(), request.seatIds());

                // 4. Validate all requested seats exist and belong to this show
                if (showSeats.size() != request.seatIds().size()) {
                    throw new InvalidBookingException("One or more seats do not exist or do not belong to this show");
                }

                // 5. Check if all seats are available
                var unavailableSeats = showSeats.stream()
                        .filter(seat -> seat.getStatus() != ShowSeat.SeatStatus.AVAILABLE)
                        .toList();

                if (!unavailableSeats.isEmpty()) {
                    throw new SeatUnavailableException();
                }

                // 6. Calculate total amount
                var totalAmount = showSeats.stream()
                        .map(ShowSeat::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 7. Create booking
                var booking = Booking.builder()
                        .user(user)
                        .show(show)
                        .bookingTime(LocalDateTime.now())
                        .status(Booking.BookingStatus.CONFIRMED)
                        .totalAmount(totalAmount)
                        .build();
                booking = bookingRepository.save(booking);

                // 8. Update seat status to BOOKED (bulk operation)
                for (var showSeat : showSeats) {
                    showSeat.setStatus(ShowSeat.SeatStatus.BOOKED);
                }
                showSeatRepository.saveAll(showSeats); // Bulk save instead of individual saves

                // 9. Create booking_seats junction records
                var bookingSeats = new ArrayList<BookingSeat>();
                for (var showSeat : showSeats) {
                    var bookingSeat = BookingSeat.builder()
                            .booking(booking)
                            .showSeat(showSeat)
                            .build();
                    bookingSeats.add(bookingSeat);
                }
                bookingSeatRepository.saveAll(bookingSeats);

                log.info("Booking completed successfully. Booking ID: {}", booking.getId());

                metricsConfig.getBookingSuccessCounter().increment();
                return mapToBookingResponse(booking, showSeats);
            } catch (Exception e) {
                metricsConfig.getBookingFailureCounter().increment();
                throw e;
            }
        });
    }

    public List<BookingResponse> getUserBookings(String userEmail) {
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        var bookings = bookingRepository.findByUserIdOrderByBookingTimeDesc(user.getId());

        return bookings.stream()
                .map(booking -> {
                    var showSeats = booking.getBookingSeats().stream()
                            .map(BookingSeat::getShowSeat)
                            .toList();
                    return mapToBookingResponse(booking, showSeats);
                })
                .toList();
    }

    /**
     * Get user bookings with pagination support (optimized to avoid N+1 queries)
     * 
     * @param userEmail - User's email
     * @param pageable  - Pagination parameters (page number, size, sort)
     * @return Page of BookingResponse
     */
    public Page<BookingResponse> getUserBookingsPaginated(String userEmail, Pageable pageable) {
        log.info("Fetching paginated bookings for user: {} with page: {}, size: {}",
                userEmail, pageable.getPageNumber(), pageable.getPageSize());

        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        // First, get the paginated booking IDs
        var bookingsPage = bookingRepository.findByUserIdOrderByBookingTimeDesc(user.getId(), pageable);

        // Then fetch full details with joins to avoid N+1 queries
        if (bookingsPage.hasContent()) {
            var bookingIds = bookingsPage.getContent().stream()
                    .map(Booking::getId)
                    .toList();

            var bookingsWithDetails = bookingRepository.findByIdInWithDetails(bookingIds);

            // Map to response using fetched data
            var responses = bookingsWithDetails.stream()
                    .map(booking -> {
                        var showSeats = booking.getBookingSeats().stream()
                                .map(BookingSeat::getShowSeat)
                                .toList();
                        return mapToBookingResponse(booking, showSeats);
                    })
                    .toList();

            return new org.springframework.data.domain.PageImpl<>(
                    responses,
                    pageable,
                    bookingsPage.getTotalElements());
        }

        return Page.empty(pageable);
    }

    @Transactional
    public CancellationResponse cancelBooking(Long bookingId, String userEmail) {
        // 1. Get user
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        // 2. Get booking
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // 3. Verify booking belongs to user
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedBookingAccessException();
        }

        // 4. Check if already cancelled
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new InvalidBookingException("Booking is already cancelled");
        }

        // 5. Check 24-hour cancellation policy
        var showStartTime = booking.getShow().getStartTime();
        var now = LocalDateTime.now();
        var hoursUntilShow = java.time.Duration.between(now, showStartTime).toHours();

        if (hoursUntilShow < 24) {
            throw new InvalidBookingException("Cancellation must be made at least 24 hours before show time");
        }

        // 6. Process Refund (New Step)
        if (booking.getTransactionId() != null) {
            try {
                paymentService.processRefund(booking.getTransactionId(), booking.getTotalAmount());
            } catch (PaymentFailedException e) {
                log.error("Refund failed for booking {}: {}", bookingId, e.getMessage());
                throw new InvalidBookingException(
                        "Cancellation failed: Refund processing failed. Please contact support.");
            }
        }

        // 7. Release seats back to AVAILABLE
        var bookingSeats = booking.getBookingSeats();
        for (var bookingSeat : bookingSeats) {
            var showSeat = bookingSeat.getShowSeat();
            showSeat.setStatus(ShowSeat.SeatStatus.AVAILABLE);
            // Ensure locks are cleared if any
            showSeat.setLockedUntil(null);
            showSeat.setLockedByUserId(null);
        }
        // Bulk save
        var showSeatsToRelease = bookingSeats.stream()
                .map(BookingSeat::getShowSeat)
                .toList();
        showSeatRepository.saveAll(showSeatsToRelease);

        // 8. Update booking status
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // 9. Send Notification (New Step)
        notificationManager.sendBookingCancellation(booking);

        log.info("Booking cancelled and refunded successfully. Booking ID: {}", bookingId);

        return CancellationResponse.builder()
                .bookingId(bookingId)
                .status("CANCELLED")
                .message("Booking cancelled successfully. Seats have been released.")
                .cancelledAt(now)
                .build();
    }

    private BookingResponse mapToBookingResponse(Booking booking, List<ShowSeat> showSeats) {
        var seatInfos = showSeats.stream()
                .map(showSeat -> new BookingResponse.SeatInfo(
                        showSeat.getSeat().getRowNumber(),
                        showSeat.getSeat().getSeatNumber(),
                        showSeat.getPrice()))
                .toList();

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .showId(booking.getShow().getId())
                .movieTitle(booking.getShow().getMovie().getTitle())
                .showTime(booking.getShow().getStartTime())
                .seats(seatInfos)
                .totalAmount(booking.getTotalAmount())
                .bookingTime(booking.getBookingTime())
                .status(booking.getStatus().name())
                .build();
    }
}
