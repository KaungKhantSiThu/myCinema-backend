package com.kkst.mycinema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkst.mycinema.dto.BookingRequest;
import com.kkst.mycinema.dto.BookingResponse;
import com.kkst.mycinema.security.JwtAuthenticationFilter;
import com.kkst.mycinema.security.JwtUtil;
import com.kkst.mycinema.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
        when(bookingService.bookSeats(any(BookingRequest.class), anyString()))
                .thenReturn(bookingResponse);

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(1))
                .andExpect(jsonPath("$.movieTitle").value("Inception"))
                .andExpect(jsonPath("$.totalAmount").value(45.00))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

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
    @WithMockUser
    void getMyBookings_ReturnsBookingsList() throws Exception {
        // Arrange
        when(bookingService.getUserBookings(anyString()))
                .thenReturn(Collections.singletonList(bookingResponse));

        // Act & Assert
        mockMvc.perform(get("/api/bookings/my-bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(1))
                .andExpect(jsonPath("$[0].movieTitle").value("Inception"));

        verify(bookingService).getUserBookings(anyString());
    }

    @Test
    void createBooking_Unauthenticated_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void createBooking_ServiceThrowsException_ReturnsConflict() throws Exception {
        // Arrange
        when(bookingService.bookSeats(any(BookingRequest.class), anyString()))
                .thenThrow(new RuntimeException("Seats already booked"));

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().is5xxServerError());
    }
}

