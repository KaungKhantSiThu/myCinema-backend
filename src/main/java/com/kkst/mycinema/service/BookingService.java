package com.kkst.mycinema.service;

import com.kkst.mycinema.config.MetricsConfig;
import com.kkst.mycinema.dto.BookingRequest;
import com.kkst.mycinema.dto.BookingResponse;
import com.kkst.mycinema.dto.CancellationResponse;
import com.kkst.mycinema.entity.*;
import com.kkst.mycinema.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private final MetricsConfig metricsConfig;

    /**
     * CRITICAL METHOD: Books seats with optimistic locking to prevent double booking
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
                        .orElseThrow(() -> new RuntimeException("Show not found"));

                if (show.getStartTime().isBefore(LocalDateTime.now())) {
                    throw new RuntimeException("Cannot book seats for past shows");
                }

                // 2. Get user
                var user = userRepository.findByEmail(userEmail)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                // 3. Fetch seats with optimistic lock
                var showSeats = showSeatRepository.findByShowIdAndIdIn(
                        request.showId(), request.seatIds());

                // 4. Validate all requested seats exist and belong to this show
                if (showSeats.size() != request.seatIds().size()) {
                    throw new RuntimeException("One or more seats do not exist or do not belong to this show");
                }

                // 5. Check if all seats are available
                var unavailableSeats = showSeats.stream()
                        .filter(seat -> seat.getStatus() != ShowSeat.SeatStatus.AVAILABLE)
                        .toList();

                if (!unavailableSeats.isEmpty()) {
                    throw new RuntimeException("One or more selected seats are not available");
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

                // 8. Update seat status to BOOKED
                for (var showSeat : showSeats) {
                    showSeat.setStatus(ShowSeat.SeatStatus.BOOKED);
                    showSeatRepository.save(showSeat);
                }

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
                .orElseThrow(() -> new RuntimeException("User not found"));

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
     * Get user bookings with pagination support
     * @param userEmail - User's email
     * @param pageable - Pagination parameters (page number, size, sort)
     * @return Page of BookingResponse
     */
    public Page<BookingResponse> getUserBookingsPaginated(String userEmail, Pageable pageable) {
        log.info("Fetching paginated bookings for user: {} with page: {}, size: {}",
                userEmail, pageable.getPageNumber(), pageable.getPageSize());

        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var bookingsPage = bookingRepository.findByUserIdOrderByBookingTimeDesc(user.getId(), pageable);

        return bookingsPage.map(booking -> {
            var showSeats = booking.getBookingSeats().stream()
                    .map(BookingSeat::getShowSeat)
                    .toList();
            return mapToBookingResponse(booking, showSeats);
        });
    }

    /**
     * Cancel a booking with business rules
     * - Can only cancel your own bookings
     * - Must cancel at least 24 hours before show time
     * - Releases seats back to AVAILABLE status
     */
    @Transactional
    public CancellationResponse cancelBooking(Long bookingId, String userEmail) {
        log.info("Cancellation requested for booking ID: {} by user: {}", bookingId, userEmail);

        // 1. Get user
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Get booking
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // 3. Verify ownership
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You can only cancel your own bookings");
        }

        // 4. Check if already cancelled
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }

        // 5. Check 24-hour cancellation policy
        var showStartTime = booking.getShow().getStartTime();
        var now = LocalDateTime.now();
        var hoursUntilShow = java.time.Duration.between(now, showStartTime).toHours();

        if (hoursUntilShow < 24) {
            throw new RuntimeException("Cancellation must be made at least 24 hours before show time");
        }

        // 6. Release seats back to AVAILABLE
        var bookingSeats = booking.getBookingSeats();
        for (var bookingSeat : bookingSeats) {
            var showSeat = bookingSeat.getShowSeat();
            showSeat.setStatus(ShowSeat.SeatStatus.AVAILABLE);
            showSeatRepository.save(showSeat);
        }

        // 7. Update booking status
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        log.info("Booking cancelled successfully. Booking ID: {}", bookingId);

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
                        showSeat.getPrice()
                ))
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
