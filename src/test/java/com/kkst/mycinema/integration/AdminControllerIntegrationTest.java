package com.kkst.mycinema.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkst.mycinema.dto.CreateMovieRequest;
import com.kkst.mycinema.dto.CreateShowRequest;
import com.kkst.mycinema.entity.*;
import com.kkst.mycinema.repository.*;
import com.kkst.mycinema.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private MovieRepository movieRepository;
        @Autowired
        private HallRepository hallRepository;
        @Autowired
        private ShowRepository showRepository;
        @Autowired
        private SeatRepository seatRepository;
        @Autowired
        private ShowSeatRepository showSeatRepository;

        private Long movieId;
        private Long hallId;

        @BeforeEach
        void setUp() {
                showSeatRepository.deleteAll();
                showRepository.deleteAll();
                seatRepository.deleteAll();
                hallRepository.deleteAll();
                movieRepository.deleteAll();

                // Setup Data
                var movie = movieRepository
                                .save(Movie.builder().title("Inception").durationMinutes(148).genre("Sci-Fi").build());
                movieId = movie.getId();

                var hall = hallRepository.save(Hall.builder().name("Hall 1").totalRows(10).totalColumns(10).build());
                hallId = hall.getId();

                // Create dummy seats for hall to avoid other errors
                seatRepository.save(Seat.builder().hall(hall).rowNumber(1).seatNumber(1).build());
        }

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void createShow_Success() throws Exception {
                CreateShowRequest request = new CreateShowRequest(movieId, hallId, LocalDateTime.now().plusHours(1),
                                LocalDateTime.now().plusHours(3));

                mockMvc.perform(post("/api/admin/shows")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void createShow_Overlap_ReturnsConflict() throws Exception {
                // 1. Create Initial Show
                Show show = Show.builder()
                                .movie(movieRepository.findById(movieId).orElseThrow())
                                .hall(hallRepository.findById(hallId).orElseThrow())
                                .startTime(LocalDateTime.now().plusHours(1))
                                .endTime(LocalDateTime.now().plusHours(3))
                                .build();
                showRepository.save(show);

                // 2. Try to create overlapping show
                CreateShowRequest request = new CreateShowRequest(movieId, hallId, LocalDateTime.now().plusHours(2),
                                LocalDateTime.now().plusHours(4));

                mockMvc.perform(post("/api/admin/shows")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict()); // 409
        }
}
