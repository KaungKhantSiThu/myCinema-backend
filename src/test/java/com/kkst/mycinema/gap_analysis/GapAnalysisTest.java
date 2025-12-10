package com.kkst.mycinema.gap_analysis;

import com.kkst.mycinema.dto.CreateShowRequest;
import com.kkst.mycinema.entity.*;
import com.kkst.mycinema.exception.InvalidBookingException;
import com.kkst.mycinema.notification.NotificationManager;
import com.kkst.mycinema.payment.PaymentService;
import com.kkst.mycinema.repository.*;
import com.kkst.mycinema.service.AdminService;
import com.kkst.mycinema.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GapAnalysisTest {

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
    @Mock
    private SeatHoldRepository seatHoldRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private NotificationManager notificationManager;

    @Mock
    private MovieRepository movieRepository;
    @Mock
    private HallRepository hallRepository;
    @Mock
    private SeatRepository seatRepository;

    // We can't easily inject @Value fields with simple @InjectMocks,
    // but luckily we aren't testing methods that use it.
    @InjectMocks
    private BookingService bookingService;
    @InjectMocks
    private AdminService adminService;

    @Test
    void testCancelBookingTriggersRefundAndNotification() {
        // Arrange
        String userEmail = "test@example.com";
        Long bookingId = 100L;
        String transactionId = "TX-123";
        BigDecimal amount = new BigDecimal("50.00");

        User user = User.builder().id(1L).email(userEmail).build();
        Show show = Show.builder().startTime(LocalDateTime.now().plusDays(2)).build(); // Future show > 24h

        Booking booking = Booking.builder()
                .id(bookingId)
                .user(user)
                .show(show)
                .status(Booking.BookingStatus.CONFIRMED)
                .totalAmount(amount)
                .transactionId(transactionId)
                .bookingSeats(List.of()) // Empty list for simplicity
                .build();

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // Act
        bookingService.cancelBooking(bookingId, userEmail);

        // Assert
        // Verify refund was called
        verify(paymentService, times(1)).processRefund(transactionId, amount);

        // Verify notification was sent
        verify(notificationManager, times(1)).sendBookingCancellation(booking);

        // Verify booking status updated
        assertEquals(Booking.BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void testCreateShowFiltersMaintenanceSeats() {
        // Arrange
        CreateShowRequest request = new CreateShowRequest(
                1L, 1L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));

        Movie movie = Movie.builder().id(1L).build();
        Hall hall = Hall.builder().id(1L).build();

        Seat activeSeat = Seat.builder().id(10L).seatNumber(1).status(Seat.SeatStatus.ACTIVE).build();
        Seat maintenanceSeat = Seat.builder().id(11L).seatNumber(2).status(Seat.SeatStatus.MAINTENANCE).build();

        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));
        when(seatRepository.findByHallId(1L)).thenReturn(List.of(activeSeat, maintenanceSeat));
        when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        adminService.createShow(request);

        // Assert
        // Verify we only saved ONE ShowSeat (the active one)
        verify(showSeatRepository).saveAll(argThat(list -> {
            if (list instanceof List) {
                List<ShowSeat> seats = (List<ShowSeat>) list;
                return seats.size() == 1 && seats.get(0).getSeat().getId().equals(10L);
            }
            return false;
        }));
    }
}
