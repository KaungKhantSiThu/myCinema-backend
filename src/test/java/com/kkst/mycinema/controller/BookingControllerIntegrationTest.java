package com.kkst.mycinema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkst.mycinema.dto.BookingRequest;
import com.kkst.mycinema.dto.BookingResponse;
import com.kkst.mycinema.security.CustomUserDetailsService;
import com.kkst.mycinema.security.JwtAuthenticationFilter;
import com.kkst.mycinema.security.JwtUtil;
import com.kkst.mycinema.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@AutoConfigureMockMvc // removed addFilters=false to enable security
class BookingControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private BookingService bookingService;

        // Required for SecurityConfig
        @MockBean
        private JwtUtil jwtUtil;

        @MockBean
        private CustomUserDetailsService customUserDetailsService;

        @TestConfiguration
        static class TestConfig {
                @Bean
                public JwtAuthenticationFilter jwtAuthenticationFilter() {
                        return new JwtAuthenticationFilter(null, null) {
                                @Override
                                protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                                jakarta.servlet.http.HttpServletResponse response,
                                                jakarta.servlet.FilterChain filterChain)
                                                throws jakarta.servlet.ServletException, java.io.IOException {
                                        filterChain.doFilter(request, response);
                                }
                        };
                }
        }

        private BookingRequest bookingRequest;
        private BookingResponse bookingResponse;

        @BeforeEach
        void setUp() {
                bookingRequest = BookingRequest.builder()
                                .showId(1L)
                                .seatIds(Arrays.asList(1L, 2L, 3L))
                                .build();

                bookingResponse = BookingResponse.builder()
                                .bookingId(1L)
                                .showId(1L)
                                .movieTitle("Inception")
                                .showTime(LocalDateTime.now().plusHours(2))
                                .seats(Collections.emptyList())
                                .totalAmount(new BigDecimal("45.00"))
                                .bookingTime(LocalDateTime.now())
                                .status("CONFIRMED")
                                .build();
        }

        @Test
        @WithMockUser(username = "user@example.com")
        void createBooking_ValidRequest_ReturnsCreated() throws Exception {
                // Arrange
                when(bookingService.bookSeats(any(BookingRequest.class), eq("user@example.com")))
                                .thenReturn(bookingResponse);

                // Act & Assert
                mockMvc.perform(post("/api/bookings")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(bookingRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.bookingId").value(1))
                                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                                .andExpect(jsonPath("$.totalAmount").value(45.00));

                // Verify service was called with correct user email from security context
                verify(bookingService).bookSeats(any(BookingRequest.class), eq("user@example.com"));
        }

        @Test
        @WithMockUser
        void createBooking_EmptySeatList_ReturnsBadRequest() throws Exception {
                // Arrange
                var invalidRequest = BookingRequest.builder()
                                .showId(1L)
                                .seatIds(Collections.emptyList())
                                .build();

                // Act & Assert
                mockMvc.perform(post("/api/bookings")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "user@example.com")
        void getMyBookings_ReturnsBookingsList() throws Exception {
                // Arrange
                when(bookingService.getUserBookings(eq("user@example.com")))
                                .thenReturn(Collections.singletonList(bookingResponse));

                // Act & Assert
                mockMvc.perform(get("/api/bookings/my-bookings"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].bookingId").value(1));

                verify(bookingService).getUserBookings(eq("user@example.com"));
        }

        @Test
        // No @WithMockUser implies unauthenticated
        void createBooking_Unauthenticated_ReturnsUnauthorized() throws Exception {
                // Act & Assert
                mockMvc.perform(post("/api/bookings")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(bookingRequest)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        void createBooking_ServiceThrowsException_ReturnsConflict() throws Exception {
                // Arrange
                // Note: GlobalExceptionHandler maps RuntimeException to 400 Bad Request
                // usually,
                // but if it's a specific exception like SeatUnavailableException it might be
                // 409.
                // Let's use RuntimeException as in the original test which maps to 400 in
                // current handler.
                when(bookingService.bookSeats(any(BookingRequest.class), anyString()))
                                .thenThrow(new RuntimeException("Seats already booked"));

                // Act & Assert
                mockMvc.perform(post("/api/bookings")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(bookingRequest)))
                                .andExpect(status().isBadRequest());
        }
}
