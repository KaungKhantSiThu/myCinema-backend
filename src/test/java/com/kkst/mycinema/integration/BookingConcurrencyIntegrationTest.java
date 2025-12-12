package com.kkst.mycinema.integration;

import com.kkst.mycinema.dto.BookingRequest;
import com.kkst.mycinema.dto.SeatHoldResponse;
import com.kkst.mycinema.entity.*;
import com.kkst.mycinema.repository.*;
import com.kkst.mycinema.service.BookingService;
import com.kkst.mycinema.exception.SeatUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BookingConcurrencyIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ShowRepository showRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private HallRepository hallRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private ShowSeatRepository showSeatRepository;
    @Autowired
    private UserRepository userRepository;

    private Long showId;
    private Long seatId;
    private String user1Email = "user1@example.com";
    private String user2Email = "user2@example.com";

    @BeforeEach
    void setUp() {
        // Clean up
        showSeatRepository.deleteAll();
        showRepository.deleteAll();
        seatRepository.deleteAll();
        hallRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();

        // Setup Users
        createTestUser(user1Email, "User 1");
        createTestUser(user2Email, "User 2");

        // Setup Data
        var movie = movieRepository
                .save(Movie.builder().title("Inception").durationMinutes(148).genre("Sci-Fi").build());
        var hall = hallRepository.save(Hall.builder().name("Hall 1").totalRows(10).totalColumns(10).build());
        var show = showRepository.save(Show.builder().movie(movie).hall(hall)
                .startTime(LocalDateTime.now().plusHours(2)).endTime(LocalDateTime.now().plusHours(4)).build());
        showId = show.getId();

        var seat = seatRepository.save(Seat.builder().hall(hall).rowNumber(1).seatNumber(1).build());
        var showSeat = showSeatRepository.save(ShowSeat.builder().show(show).seat(seat).price(new BigDecimal("10.00"))
                .status(ShowSeat.SeatStatus.AVAILABLE).build());
        seatId = showSeat.getId();
    }

    private void createTestUser(String email, String name) {
        userRepository.save(User.builder().email(email).name(name).password("pw").roles("USER")
                .createdAt(LocalDateTime.now()).build());
    }

    @Test
    void testConcurrentSeatHolds() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Runnable task1 = () -> {
            try {
                latch.await();
                BookingRequest request = new BookingRequest(showId, List.of(seatId));
                bookingService.holdSeats(request, user1Email);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        };

        Runnable task2 = () -> {
            try {
                latch.await();
                BookingRequest request = new BookingRequest(showId, List.of(seatId));
                bookingService.holdSeats(request, user2Email);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        };

        executorService.submit(task1);
        executorService.submit(task2);

        latch.countDown(); // Start!
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // One should succeed, one should fail
        assertEquals(1, successCount.get(), "Only one hold should succeed");
        assertEquals(1, failureCount.get(), "One hold should fail");

        // Verify DB state
        var seat = showSeatRepository.findById(seatId).orElseThrow();
        assertTrue(seat.isLocked(), "Seat should be locked");
    }

    @Test
    void testConcurrentBookings() throws InterruptedException {
        // Test direct booking concurrency
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Runnable task1 = () -> {
            try {
                latch.await();
                BookingRequest request = new BookingRequest(showId, List.of(seatId));
                bookingService.bookSeats(request, user1Email);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        };

        Runnable task2 = () -> {
            try {
                latch.await();
                BookingRequest request = new BookingRequest(showId, List.of(seatId));
                bookingService.bookSeats(request, user2Email);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        };

        executorService.submit(task1);
        executorService.submit(task2);

        latch.countDown();
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(1, successCount.get(), "Only one booking should succeed");
        assertEquals(1, failureCount.get(), "One booking should fail");

        var seat = showSeatRepository.findById(seatId).orElseThrow();
        assertEquals(ShowSeat.SeatStatus.BOOKED, seat.getStatus());
    }
}
