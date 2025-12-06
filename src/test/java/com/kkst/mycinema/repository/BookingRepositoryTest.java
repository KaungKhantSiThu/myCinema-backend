package com.kkst.mycinema.repository;

import com.kkst.mycinema.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookingRepository bookingRepository;

    private User testUser;
    private Show testShow;

    @BeforeEach
    void setUp() {
        // Create user
        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .roles("ROLE_USER")
                .createdAt(LocalDateTime.now())
                .build();
        testUser = entityManager.persist(testUser);

        // Create hall
        Hall hall = Hall.builder()
                .name("IMAX Hall 1")
                .totalRows(10)
                .totalColumns(10)
                .build();
        hall = entityManager.persist(hall);

        // Create movie
        Movie movie = Movie.builder()
                .title("Test Movie")
                .durationMinutes(120)
                .genre("Action")
                .build();
        movie = entityManager.persist(movie);

        // Create show
        testShow = Show.builder()
                .movie(movie)
                .hall(hall)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .build();
        testShow = entityManager.persist(testShow);

        entityManager.flush();
    }

    @Test
    void findByUserId_ReturnsUserBookings() {
        // Arrange
        Booking booking1 = Booking.builder()
                .user(testUser)
                .show(testShow)
                .bookingTime(LocalDateTime.now())
                .status(Booking.BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("30.00"))
                .build();
        entityManager.persist(booking1);

        Booking booking2 = Booking.builder()
                .user(testUser)
                .show(testShow)
                .bookingTime(LocalDateTime.now().minusHours(1))
                .status(Booking.BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("45.00"))
                .build();
        entityManager.persist(booking2);

        entityManager.flush();

        // Act
        List<Booking> result = bookingRepository.findByUserId(testUser.getId());

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(b -> b.getUser().getId().equals(testUser.getId()));
    }

    @Test
    void findByUserIdOrderByBookingTimeDesc_ReturnsSortedBookings() {
        // Arrange
        Booking oldBooking = Booking.builder()
                .user(testUser)
                .show(testShow)
                .bookingTime(LocalDateTime.now().minusDays(2))
                .status(Booking.BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("30.00"))
                .build();
        entityManager.persist(oldBooking);

        Booking newBooking = Booking.builder()
                .user(testUser)
                .show(testShow)
                .bookingTime(LocalDateTime.now())
                .status(Booking.BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("45.00"))
                .build();
        entityManager.persist(newBooking);

        entityManager.flush();

        // Act
        List<Booking> result = bookingRepository.findByUserIdOrderByBookingTimeDesc(testUser.getId());

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBookingTime()).isAfter(result.get(1).getBookingTime());
    }

    @Test
    void save_PersistsBookingWithRelationships() {
        // Arrange
        Booking booking = Booking.builder()
                .user(testUser)
                .show(testShow)
                .bookingTime(LocalDateTime.now())
                .status(Booking.BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("60.00"))
                .build();

        // Act
        Booking saved = bookingRepository.save(booking);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Booking found = bookingRepository.findById(saved.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(found.getShow().getId()).isEqualTo(testShow.getId());
        assertThat(found.getTotalAmount()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    void findByUserId_EmptyResult_WhenNoBookings() {
        // Act
        List<Booking> result = bookingRepository.findByUserId(testUser.getId());

        // Assert
        assertThat(result).isEmpty();
    }
}

