package com.kkst.mycinema.controller;

import com.kkst.mycinema.dto.BookingRequest;
import com.kkst.mycinema.dto.BookingResponse;
import com.kkst.mycinema.dto.CancellationResponse;
import com.kkst.mycinema.dto.PaymentConfirmationRequest;
import com.kkst.mycinema.dto.SeatHoldResponse;
import com.kkst.mycinema.service.BookingService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Cinema seat booking management APIs")
@SecurityRequirement(name = "bearer-jwt")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/hold")
    @RateLimiter(name = "booking")
    @Operation(
            summary = "Hold seats temporarily",
            description = "Holds seats for a limited time (default 10 minutes) while user completes payment. " +
                         "Held seats are automatically released if not confirmed."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seats held successfully"),
            @ApiResponse(responseCode = "409", description = "Seats already held or booked"),
            @ApiResponse(responseCode = "429", description = "Too many requests - rate limit exceeded")
    })
    public ResponseEntity<SeatHoldResponse> holdSeats(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        var userEmail = authentication.getName();
        var response = bookingService.holdSeats(request, userEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm-with-payment")
    @RateLimiter(name = "booking")
    @Operation(
            summary = "Confirm held seats with payment (Production)",
            description = "Confirms a seat hold after processing payment. This is the recommended production endpoint. " +
                         "Must be called before hold expires. Sends confirmation email on success."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Booking confirmed and payment processed successfully"),
            @ApiResponse(responseCode = "400", description = "Hold expired or invalid"),
            @ApiResponse(responseCode = "402", description = "Payment failed"),
            @ApiResponse(responseCode = "429", description = "Too many requests - rate limit exceeded")
    })
    public ResponseEntity<BookingResponse> confirmBookingWithPayment(
            @Valid @RequestBody PaymentConfirmationRequest request,
            Authentication authentication) {
        var userEmail = authentication.getName();
        var response = bookingService.confirmHoldWithPayment(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/confirm/{holdToken}")
    @RateLimiter(name = "booking")
    @Operation(
            summary = "Confirm held seats (Legacy/Testing)",
            description = "Confirms a seat hold without payment processing. Use /confirm-with-payment for production."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Booking confirmed successfully"),
            @ApiResponse(responseCode = "400", description = "Hold expired or invalid"),
            @ApiResponse(responseCode = "429", description = "Too many requests - rate limit exceeded")
    })
    @Deprecated
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable String holdToken,
            Authentication authentication) {
        var userEmail = authentication.getName();
        var response = bookingService.confirmHold(holdToken, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/hold/{holdToken}")
    @Operation(
            summary = "Release held seats",
            description = "Releases seats that were previously held, making them available again."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seats released successfully"),
            @ApiResponse(responseCode = "404", description = "Hold not found or already expired")
    })
    public ResponseEntity<Void> releaseHold(
            @PathVariable String holdToken,
            Authentication authentication) {
        var userEmail = authentication.getName();
        bookingService.releaseHold(holdToken, userEmail);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @RateLimiter(name = "booking")
    @Operation(
            summary = "Book seats directly (legacy)",
            description = "Books one or more seats for a movie show directly without hold. " +
                         "Uses optimistic locking to prevent double-booking. " +
                         "Consider using /hold + /confirm for better UX."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Booking created successfully"),
            @ApiResponse(responseCode = "409", description = "Seats already booked by another user"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "429", description = "Too many requests - rate limit exceeded")
    })
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        var userEmail = authentication.getName();
        var response = bookingService.bookSeats(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-bookings")
    @Operation(
            summary = "Get user bookings",
            description = "Retrieves all bookings made by the authenticated user, ordered by booking time (newest first)"
    )
    @ApiResponse(responseCode = "200", description = "List of bookings retrieved successfully")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        var userEmail = authentication.getName();
        return ResponseEntity.ok(bookingService.getUserBookings(userEmail));
    }

    @GetMapping("/my-bookings/paginated")
    @Operation(
            summary = "Get user bookings (paginated)",
            description = "Retrieves bookings with pagination support. Use page (0-indexed), size, and sort parameters."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of bookings retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    public ResponseEntity<Page<BookingResponse>> getMyBookingsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        // Validate pagination parameters
        if (page < 0) {
            page = 0;
        }
        if (size < 1 || size > 100) {
            size = 10; // Default to 10, max 100 per page
        }

        var userEmail = authentication.getName();
        Pageable pageable = PageRequest.of(page, size);
        var bookingsPage = bookingService.getUserBookingsPaginated(userEmail, pageable);

        return ResponseEntity.ok(bookingsPage);
    }

    @DeleteMapping("/{bookingId}")
    @Operation(
            summary = "Cancel a booking",
            description = "Cancels a booking and releases the seats. Must be done at least 24 hours before show time."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel within 24 hours of show time"),
            @ApiResponse(responseCode = "403", description = "Can only cancel own bookings"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<CancellationResponse> cancelBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {
        var userEmail = authentication.getName();
        var response = bookingService.cancelBooking(bookingId, userEmail);
        return ResponseEntity.ok(response);
    }
}

