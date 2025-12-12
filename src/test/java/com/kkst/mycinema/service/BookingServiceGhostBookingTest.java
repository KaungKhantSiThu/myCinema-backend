package com.kkst.mycinema.service;

import com.kkst.mycinema.dto.BookingResponse;
import com.kkst.mycinema.dto.PaymentConfirmationRequest;
import com.kkst.mycinema.payment.PaymentRequest;
import com.kkst.mycinema.payment.PaymentRequest.PaymentMethod;
import com.kkst.mycinema.entity.*;
import com.kkst.mycinema.exception.BookingFailedAfterPaymentException;
import com.kkst.mycinema.exception.SeatHoldExpiredException;
import com.kkst.mycinema.payment.PaymentResult;
import com.kkst.mycinema.payment.PaymentService;
import com.kkst.mycinema.payment.PaymentStatus;
import com.kkst.mycinema.repository.BookingRepository;
import com.kkst.mycinema.repository.BookingSeatRepository;
import com.kkst.mycinema.repository.SeatHoldRepository;
import com.kkst.mycinema.repository.ShowSeatRepository;
import com.kkst.mycinema.config.MetricsConfig;
import com.kkst.mycinema.notification.NotificationManager;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceGhostBookingTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ShowSeatRepository showSeatRepository;
    @Mock
    private SeatHoldRepository seatHoldRepository;
    @Mock
    private BookingSeatRepository bookingSeatRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private NotificationManager notificationManager;
    @Mock
    private MetricsConfig metricsConfig;
    @Mock
    private Counter counter;

    @Spy
    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Show testShow;
    private SeatHold testSeatHold;
    private ShowSeat testShowSeat;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        testShow = Show.builder()
                .id(1L)
                .movie(Movie.builder().title("Test Movie").build())
                .startTime(LocalDateTime.now().plusHours(1))
                .build();

        testShowSeat = ShowSeat.builder()
                .id(100L)
                .price(new BigDecimal("10.00"))
                .seat(Seat.builder().rowNumber(1).seatNumber(1).build())
                .status(ShowSeat.SeatStatus.LOCKED)
                .lockedByUserId(1L)
                .lockedUntil(LocalDateTime.now().plusMinutes(5))
                .build();

        testSeatHold = SeatHold.builder()
                .id(55L)
                .holdToken("valid-token")
                .user(testUser)
                .show(testShow)
                .seatIds("100")
                .status(SeatHold.HoldStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        lenient().when(metricsConfig.getBookingSuccessCounter()).thenReturn(counter);
    }

    @Test
    void confirmHoldWithPayment_Success() {
        // Arrange
        String holdToken = "valid-token";
        String transactionId = "tx-123";
        PaymentConfirmationRequest request = PaymentConfirmationRequest.builder()
                .holdToken(holdToken)
                .paymentMethod(PaymentMethod.CARD)
                .build();

        // 1. Mock Initiate Payment (return hold/amount)
        // We need to spy or mock the internal calls or just mock the repositories such
        // that the logic flows
        // Since initiatePayment is public/transactional, normal mocking applies if we
        // called it externally.
        // But here we are calling a method that calls it.
        // We'll mock the repositories to support initiatePayment logic.

        when(seatHoldRepository.findByHoldToken(holdToken)).thenReturn(Optional.of(testSeatHold));
        when(seatHoldRepository.save(any(SeatHold.class))).thenAnswer(i -> i.getArguments()[0]);

        when(showSeatRepository.findByShowIdAndIdIn(any(), any())).thenReturn(List.of(testShowSeat));

        // 2. Mock Payment Service
        PaymentResult successResult = PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .build();
        when(paymentService.processBookingPayment(any(), eq(holdToken), any())).thenReturn(successResult);

        // 3. Mock Complete Booking
        when(seatHoldRepository.findById(55L)).thenReturn(Optional.of(testSeatHold));

        Booking savedBooking = Booking.builder()
                .id(999L)
                .user(testUser)
                .show(testShow)
                .totalAmount(new BigDecimal("10.00"))
                .status(Booking.BookingStatus.CONFIRMED)
                .bookingTime(LocalDateTime.now())
                .build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // Act
        BookingResponse response = bookingService.confirmHoldWithPayment(request, "test@example.com");

        // Assert
        assertNotNull(response);
        assertEquals("CONFIRMED", response.status());
        verify(paymentService, never()).processRefund(any(), any());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void confirmHoldWithPayment_BookingFails_TriggersRefund() {
        // Arrange
        String holdToken = "valid-token";
        String transactionId = "tx-123";
        PaymentConfirmationRequest request = PaymentConfirmationRequest.builder()
                .holdToken(holdToken)
                .paymentMethod(PaymentMethod.CARD)
                .build();

        // 1. Mock Initiate works
        when(seatHoldRepository.findByHoldToken(holdToken)).thenReturn(Optional.of(testSeatHold));
        when(seatHoldRepository.save(any(SeatHold.class))).thenAnswer(i -> i.getArguments()[0]);
        when(showSeatRepository.findByShowIdAndIdIn(any(), any())).thenReturn(List.of(testShowSeat));

        // 2. Mock Payment works
        PaymentResult successResult = PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .build();
        when(paymentService.processBookingPayment(any(), eq(holdToken), any())).thenReturn(successResult);

        // 3. Mock Complete Booking FAILS with OptimisticLockingFailureException
        // We assume completeBooking reads the hold again
        when(seatHoldRepository.findById(55L))
                .thenThrow(new OptimisticLockingFailureException("Simulated concurrency error"));

        // Act & Assert
        BookingFailedAfterPaymentException exception = assertThrows(BookingFailedAfterPaymentException.class, () -> {
            bookingService.confirmHoldWithPayment(request, "test@example.com");
        });

        assertTrue(exception.getMessage().contains("Automatic refund has been initiated"));

        // VERIFY REFUND WAS CALLED
        verify(paymentService).processRefund(eq(transactionId), any());
    }
}
