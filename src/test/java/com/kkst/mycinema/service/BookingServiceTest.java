package com.kkst.mycinema.service;

import com.kkst.mycinema.dto.BookingRequest;
import com.kkst.mycinema.entity.*;
import com.kkst.mycinema.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ShowSeatRepository showSeatRepository;

    @Mock
    private ShowRepository showRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Show testShow;
    private Movie testMovie;
    private Hall testHall;
    private Seat testSeat1;
    private Seat testSeat2;
    private ShowSeat testShowSeat1;
    private ShowSeat testShowSeat2;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("hashedPassword")
                .roles("ROLE_USER")
                .createdAt(LocalDateTime.now())
                .build();

        // Create test hall
        testHall = Hall.builder()
                .id(1L)
                .name("IMAX Hall 1")
                .totalRows(10)
                .totalColumns(10)
                .build();

        // Create test movie
        testMovie = Movie.builder()
                .id(1L)
                .title("Inception")
                .durationMinutes(148)
                .genre("Sci-Fi")
                .build();

        // Create test show (2 hours in the future)
        testShow = Show.builder()
                .id(1L)
                .movie(testMovie)
                .hall(testHall)
                .startTime(LocalDateTime.now().plusHours(2))
                .endTime(LocalDateTime.now().plusHours(4))
                .build();

        // Create test seats
        testSeat1 = Seat.builder()
                .id(1L)
                .hall(testHall)
                .rowNumber(1)
                .seatNumber(1)
                .build();

        testSeat2 = Seat.builder()
                .id(2L)
                .hall(testHall)
                .rowNumber(1)
                .seatNumber(2)
                .build();

        // Create test show seats
        testShowSeat1 = ShowSeat.builder()
                .id(1L)
                .show(testShow)
                .seat(testSeat1)
                .status(ShowSeat.SeatStatus.AVAILABLE)
                .price(new BigDecimal("15.00"))
                .version(0L)
                .build();

        testShowSeat2 = ShowSeat.builder()
                .id(2L)
                .show(testShow)
                .seat(testSeat2)
                .status(ShowSeat.SeatStatus.AVAILABLE)
                .price(new BigDecimal("15.00"))
                .version(0L)
                .build();
    }

    @Test
    void bookSeats_Success() {
        // Arrange
        var request = BookingRequest.builder()
                .showId(1L)
                .seatIds(List.of(1L, 2L))
                .build();

        var showSeats = List.of(testShowSeat1, testShowSeat2);

        when(showRepository.findById(1L)).thenReturn(Optional.of(testShow));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(showSeatRepository.findByShowIdAndIdIn(1L, List.of(1L, 2L))).thenReturn(showSeats);

        var savedBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .show(testShow)
                .bookingTime(LocalDateTime.now())
                .status(Booking.BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("30.00"))
                .bookingSeats(new ArrayList<>())
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(showSeatRepository.save(any(ShowSeat.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        var response = bookingService.bookSeats(request, "test@example.com");

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.bookingId());
        assertEquals(1L, response.showId());
        assertEquals("Inception", response.movieTitle());
        assertEquals(2, response.seats().size());
        assertEquals(new BigDecimal("30.00"), response.totalAmount());
        assertEquals("CONFIRMED", response.status());

        // Verify seat status was updated to BOOKED
        verify(showSeatRepository, times(2)).save(any(ShowSeat.class));
        assertEquals(ShowSeat.SeatStatus.BOOKED, testShowSeat1.getStatus());
        assertEquals(ShowSeat.SeatStatus.BOOKED, testShowSeat2.getStatus());
    }

    @Test
    void bookSeats_ShowNotFound_ThrowsException() {
        // Arrange
        var request = BookingRequest.builder()
                .showId(999L)
                .seatIds(List.of(1L, 2L))
                .build();

        when(showRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        var exception = assertThrows(RuntimeException.class,
            () -> bookingService.bookSeats(request, "test@example.com"));

        assertEquals("Show not found", exception.getMessage());
    }

    @Test
    void bookSeats_PastShow_ThrowsException() {
        // Arrange
        var pastShow = Show.builder()
                .id(1L)
                .movie(testMovie)
                .hall(testHall)
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().minusMinutes(30))
                .build();

        var request = BookingRequest.builder()
                .showId(1L)
                .seatIds(List.of(1L, 2L))
                .build();

        when(showRepository.findById(1L)).thenReturn(Optional.of(pastShow));

        // Act & Assert
        var exception = assertThrows(RuntimeException.class,
            () -> bookingService.bookSeats(request, "test@example.com"));

        assertEquals("Cannot book seats for past shows", exception.getMessage());
    }

    @Test
    void bookSeats_UserNotFound_ThrowsException() {
        // Arrange
        var request = BookingRequest.builder()
                .showId(1L)
                .seatIds(List.of(1L, 2L))
                .build();

        when(showRepository.findById(1L)).thenReturn(Optional.of(testShow));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        var exception = assertThrows(RuntimeException.class,
            () -> bookingService.bookSeats(request, "test@example.com"));

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void bookSeats_SeatsMismatch_ThrowsException() {
        // Arrange
        var request = BookingRequest.builder()
                .showId(1L)
                .seatIds(List.of(1L, 2L, 3L)) // Request 3 seats
                .build();

        var showSeats = List.of(testShowSeat1, testShowSeat2); // Only 2 found

        when(showRepository.findById(1L)).thenReturn(Optional.of(testShow));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(showSeatRepository.findByShowIdAndIdIn(1L, List.of(1L, 2L, 3L))).thenReturn(showSeats);

        // Act & Assert
        var exception = assertThrows(RuntimeException.class,
            () -> bookingService.bookSeats(request, "test@example.com"));

        assertEquals("One or more seats do not exist or do not belong to this show",
                    exception.getMessage());
    }

    @Test
    void bookSeats_SeatNotAvailable_ThrowsException() {
        // Arrange
        testShowSeat1.setStatus(ShowSeat.SeatStatus.BOOKED); // Already booked

        var request = BookingRequest.builder()
                .showId(1L)
                .seatIds(List.of(1L, 2L))
                .build();

        var showSeats = List.of(testShowSeat1, testShowSeat2);

        when(showRepository.findById(1L)).thenReturn(Optional.of(testShow));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(showSeatRepository.findByShowIdAndIdIn(1L, List.of(1L, 2L))).thenReturn(showSeats);

        // Act & Assert
        var exception = assertThrows(RuntimeException.class,
            () -> bookingService.bookSeats(request, "test@example.com"));

        assertEquals("One or more selected seats are not available", exception.getMessage());
    }

    @Test
    void getUserBookings_Success() {
        // Arrange
        var booking = Booking.builder()
                .id(1L)
                .user(testUser)
                .show(testShow)
                .bookingTime(LocalDateTime.now())
                .status(Booking.BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("30.00"))
                .bookingSeats(new ArrayList<>())
                .build();

        var bookingSeat1 = BookingSeat.builder()
                .id(1L)
                .booking(booking)
                .showSeat(testShowSeat1)
                .build();

        var bookingSeat2 = BookingSeat.builder()
                .id(2L)
                .booking(booking)
                .showSeat(testShowSeat2)
                .build();

        booking.getBookingSeats().add(bookingSeat1);
        booking.getBookingSeats().add(bookingSeat2);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(bookingRepository.findByUserIdOrderByBookingTimeDesc(1L)).thenReturn(List.of(booking));

        // Act
        var bookings = bookingService.getUserBookings("test@example.com");

        // Assert
        assertNotNull(bookings);
        assertEquals(1, bookings.size());

        var response = bookings.get(0);
        assertEquals(1L, response.bookingId());
        assertEquals("Inception", response.movieTitle());
        assertEquals(2, response.seats().size());
        assertEquals(new BigDecimal("30.00"), response.totalAmount());
    }

    @Test
    void getUserBookings_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        var exception = assertThrows(RuntimeException.class,
            () -> bookingService.getUserBookings("test@example.com"));

        assertEquals("User not found", exception.getMessage());
    }
}

