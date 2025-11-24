package com.kkst.mycinema.controller;

import com.kkst.mycinema.dto.BookingRequest;
import com.kkst.mycinema.dto.BookingResponse;
import com.kkst.mycinema.dto.CancellationResponse;
import com.kkst.mycinema.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @PostMapping
    @Operation(
            summary = "Book seats for a show",
            description = "Books one or more seats for a movie show. Uses optimistic locking to prevent double-booking."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Booking created successfully"),
            @ApiResponse(responseCode = "409", description = "Seats already booked by another user"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
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

