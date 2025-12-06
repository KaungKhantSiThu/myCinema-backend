package com.kkst.mycinema.service;

import com.kkst.mycinema.entity.*;
import com.kkst.mycinema.exception.ShowNotFoundException;
import com.kkst.mycinema.repository.ShowRepository;
import com.kkst.mycinema.repository.ShowSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

    @Mock
    private ShowRepository showRepository;

    @Mock
    private ShowSeatRepository showSeatRepository;

    @InjectMocks
    private ShowService showService;

    private Show testShow;
    private Movie testMovie;
    private Hall testHall;
    private List<ShowSeat> testShowSeats;

    @BeforeEach
    void setUp() {
        testHall = Hall.builder()
                .id(1L)
                .name("IMAX Hall 1")
                .totalRows(10)
                .totalColumns(10)
                .build();

        testMovie = Movie.builder()
                .id(1L)
                .title("Inception")
                .durationMinutes(148)
                .genre("Sci-Fi")
                .build();

        testShow = Show.builder()
                .id(1L)
                .movie(testMovie)
                .hall(testHall)
                .startTime(LocalDateTime.now().plusHours(2))
                .endTime(LocalDateTime.now().plusHours(4))
                .build();

        testShowSeats = Arrays.asList(
                ShowSeat.builder()
                        .id(1L)
                        .show(testShow)
                        .seat(Seat.builder().id(1L).rowNumber(1).seatNumber(1).hall(testHall).build())
                        .status(ShowSeat.SeatStatus.AVAILABLE)
                        .price(new BigDecimal("15.00"))
                        .version(0L)
                        .build(),
                ShowSeat.builder()
                        .id(2L)
                        .show(testShow)
                        .seat(Seat.builder().id(2L).rowNumber(1).seatNumber(2).hall(testHall).build())
                        .status(ShowSeat.SeatStatus.BOOKED)
                        .price(new BigDecimal("15.00"))
                        .version(0L)
                        .build()
        );
    }

    @Test
    void getShows_ReturnsAllShows() {
        // Arrange
        when(showRepository.findAll()).thenReturn(Collections.singletonList(testShow));

        // Act
        var result = showService.getShows(null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Inception", result.get(0).movieTitle());
        verify(showRepository).findAll();
    }

    @Test
    void getShows_WithMovieId_ReturnsFilteredShows() {
        // Arrange
        when(showRepository.findByMovieId(1L)).thenReturn(Collections.singletonList(testShow));

        // Act
        var result = showService.getShows(1L, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    void getShows_WithMovieIdAndDate_ReturnsFilteredShows() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);
        when(showRepository.findByMovieIdAndDate(1L, date))
                .thenReturn(Collections.singletonList(testShow));

        // Act
        var result = showService.getShows(1L, date);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(showRepository).findByMovieIdAndDate(1L, date);
    }

    @Test
    void getShowSeats_ReturnsSeatsGroupedByRow() {
        // Arrange
        when(showRepository.findById(1L)).thenReturn(Optional.of(testShow));
        when(showSeatRepository.findByShowId(1L))
                .thenReturn(testShowSeats);

        // Act
        var result = showService.getShowSeats(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.showId());
        assertEquals("Inception", result.movieTitle());
        assertTrue(result.seatsByRow().containsKey(1));
        assertEquals(2, result.seatsByRow().get(1).size());
    }

    @Test
    void getShowSeats_ShowNotFound_ThrowsException() {
        // Arrange
        when(showRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ShowNotFoundException.class, () -> showService.getShowSeats(999L));
    }

    @Test
    void getShowSeats_VerifiesSeatStatusMapping() {
        // Arrange
        when(showRepository.findById(1L)).thenReturn(Optional.of(testShow));
        when(showSeatRepository.findByShowId(1L))
                .thenReturn(testShowSeats);

        // Act
        var result = showService.getShowSeats(1L);

        // Assert
        var seats = result.seatsByRow().get(1);
        assertEquals("AVAILABLE", seats.get(0).status());
        assertEquals("BOOKED", seats.get(1).status());
    }

    @Test
    void getShows_EmptyList_ReturnsEmptyList() {
        // Arrange
        when(showRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        var result = showService.getShows(null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

