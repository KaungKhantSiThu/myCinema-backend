package com.kkst.mycinema.service;

import com.kkst.mycinema.dto.CreateMovieRequest;
import com.kkst.mycinema.dto.CreateShowRequest;
import com.kkst.mycinema.entity.*;
import com.kkst.mycinema.exception.ResourceConflictException;
import com.kkst.mycinema.repository.*;
import org.junit.jupiter.api.BeforeEach;
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
class AdminServiceTest {

    @Mock
    private MovieRepository movieRepository;
    @Mock
    private ShowRepository showRepository;
    @Mock
    private HallRepository hallRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private ShowSeatRepository showSeatRepository;
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private AdminService adminService;

    private Movie testMovie;
    private Hall testHall;
    private Show testShow;

    @BeforeEach
    void setUp() {
        testMovie = Movie.builder().id(1L).title("Test Movie").durationMinutes(120).build();
        testHall = Hall.builder().id(1L).name("Test Hall").build();
        testShow = Show.builder()
                .id(1L)
                .movie(testMovie)
                .hall(testHall)
                .startTime(LocalDateTime.now().plusHours(1))
                .endTime(LocalDateTime.now().plusHours(3))
                .build();
    }

    @Test
    void createShow_Success() {
        CreateShowRequest request = new CreateShowRequest(1L, 1L, LocalDateTime.now().plusHours(4),
                LocalDateTime.now().plusHours(6));

        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
        when(hallRepository.findById(1L)).thenReturn(Optional.of(testHall));
        when(showRepository.findOverlappingShows(any(), any(), any())).thenReturn(Collections.emptyList());
        when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArguments()[0]);
        when(seatRepository.findByHallId(1L)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> adminService.createShow(request));

        verify(showRepository).save(any(Show.class));
    }

    @Test
    void createShow_Overlap_ThrowsException() {
        CreateShowRequest request = new CreateShowRequest(1L, 1L, LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(3));

        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
        when(hallRepository.findById(1L)).thenReturn(Optional.of(testHall));
        when(showRepository.findOverlappingShows(any(), any(), any())).thenReturn(List.of(testShow));

        assertThrows(ResourceConflictException.class, () -> adminService.createShow(request));

        verify(showRepository, never()).save(any(Show.class));
    }

    @Test
    void updateShow_OverlapWithOther_ThrowsException() {
        Long showId = 2L; // Updating Show 2
        Show showToUpdate = Show.builder().id(showId).movie(testMovie).hall(testHall).build();
        CreateShowRequest request = new CreateShowRequest(1L, 1L, LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(3));

        when(showRepository.findById(showId)).thenReturn(Optional.of(showToUpdate));
        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
        // Overlap returns Show 1 (different ID)
        when(showRepository.findOverlappingShows(any(), any(), any())).thenReturn(List.of(testShow));

        assertThrows(ResourceConflictException.class, () -> adminService.updateShow(showId, request));
    }

    @Test
    void updateShow_OverlapWithSelf_Success() {
        Long showId = 1L; // Updating Show 1
        Show showToUpdate = Show.builder().id(showId).movie(testMovie).hall(testHall).startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(2)).build();
        CreateShowRequest request = new CreateShowRequest(1L, 1L, LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(3));

        when(showRepository.findById(showId)).thenReturn(Optional.of(showToUpdate));
        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
        // Overlap returns Show 1 (Same ID)
        when(showRepository.findOverlappingShows(any(), any(), any())).thenReturn(List.of(testShow));
        when(showRepository.save(any(Show.class))).thenAnswer(i -> i.getArguments()[0]);

        assertDoesNotThrow(() -> adminService.updateShow(showId, request));

        verify(showRepository).save(any(Show.class));
    }
}
