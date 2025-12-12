package com.kkst.mycinema.service;

import com.kkst.mycinema.config.CacheConfig;
import com.kkst.mycinema.dto.*;
import com.kkst.mycinema.entity.*;
import com.kkst.mycinema.exception.*;
import com.kkst.mycinema.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminService {

        private final MovieRepository movieRepository;
        private final ShowRepository showRepository;
        private final HallRepository hallRepository;
        private final SeatRepository seatRepository;
        private final ShowSeatRepository showSeatRepository;
        private final BookingRepository bookingRepository;

        // Movie Management
        @Transactional
        @CacheEvict(value = CacheConfig.MOVIES_CACHE, allEntries = true)
        public MovieResponse createMovie(CreateMovieRequest request) {
                log.info("Creating new movie: {}", request.title());

                var movie = Movie.builder()
                                .title(request.title())
                                .durationMinutes(request.durationMinutes())
                                .genre(request.genre())
                                .build();

                movie = movieRepository.save(movie);
                log.info("Movie created with ID: {}", movie.getId());

                return mapToMovieResponse(movie);
        }

        @Transactional
        @CacheEvict(value = CacheConfig.MOVIES_CACHE, allEntries = true)
        public MovieResponse updateMovie(Long id, CreateMovieRequest request) {
                log.info("Updating movie with ID: {}", id);

                var movie = movieRepository.findById(id)
                                .orElseThrow(() -> new MovieNotFoundException(id));

                movie.setTitle(request.title());
                movie.setDurationMinutes(request.durationMinutes());
                movie.setGenre(request.genre());

                movie = movieRepository.save(movie);
                log.info("Movie updated: {}", id);

                return mapToMovieResponse(movie);
        }

        @Transactional
        @CacheEvict(value = CacheConfig.MOVIES_CACHE, allEntries = true)
        public void deleteMovie(Long id) {
                log.info("Deleting movie with ID: {}", id);

                if (!movieRepository.existsById(id)) {
                        throw new MovieNotFoundException(id);
                }

                // Check if movie has upcoming shows
                var upcomingShows = showRepository.findByMovieId(id).stream()
                                .filter(show -> show.getStartTime().isAfter(LocalDateTime.now()))
                                .toList();

                if (!upcomingShows.isEmpty()) {
                        throw new ResourceConflictException("Cannot delete movie with upcoming shows");
                }

                movieRepository.deleteById(id);
                log.info("Movie deleted: {}", id);
        }

        // Show Management
        @Transactional
        @CacheEvict(value = { CacheConfig.SHOWS_CACHE, CacheConfig.SHOW_SEATS_CACHE }, allEntries = true)
        public ShowResponse createShow(CreateShowRequest request) {
                log.info("Creating new show for movie ID: {}", request.movieId());

                var movie = movieRepository.findById(request.movieId())
                                .orElseThrow(() -> new MovieNotFoundException(request.movieId()));

                var hall = hallRepository.findById(request.hallId())
                                .orElseThrow(() -> new HallNotFoundException(request.hallId()));

                // Validate show times
                if (request.endTime().isBefore(request.startTime())) {
                        throw new InvalidBookingException("End time must be after start time");
                }

                // Check for overlapping shows
                List<Show> overlapping = showRepository.findOverlappingShows(request.hallId(), request.startTime(),
                                request.endTime());
                if (!overlapping.isEmpty()) {
                        throw new ResourceConflictException("Show time overlaps with existing shows in the same hall");
                }

                var show = Show.builder()
                                .movie(movie)
                                .hall(hall)
                                .startTime(request.startTime())
                                .endTime(request.endTime())
                                .build();

                show = showRepository.save(show);

                // Create show seats for all seats in the hall
                var hallSeats = seatRepository.findByHallId(hall.getId());
                var showSeats = new ArrayList<ShowSeat>();

                for (var seat : hallSeats) {
                        // Skip seats under maintenance
                        if (seat.getStatus() == Seat.SeatStatus.MAINTENANCE) {
                                continue;
                        }

                        var showSeat = ShowSeat.builder()
                                        .show(show)
                                        .seat(seat)
                                        .status(ShowSeat.SeatStatus.AVAILABLE)
                                        .price(new BigDecimal("15.00")) // Default price
                                        .version(0L)
                                        .build();
                        showSeats.add(showSeat);
                }

                showSeatRepository.saveAll(showSeats);
                log.info("Show created with ID: {} with {} seats", show.getId(), showSeats.size());

                return mapToShowResponse(show);
        }

        @Transactional
        @CacheEvict(value = { CacheConfig.SHOWS_CACHE, CacheConfig.SHOW_SEATS_CACHE }, allEntries = true)
        public void deleteShow(Long id) {
                log.info("Deleting show with ID: {}", id);

                var show = showRepository.findById(id)
                                .orElseThrow(() -> new ShowNotFoundException(id));

                // Check if show has any bookings
                var bookings = bookingRepository.findAll().stream()
                                .filter(b -> b.getShow().getId().equals(id))
                                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                                .toList();

                if (!bookings.isEmpty()) {
                        throw new ResourceConflictException("Cannot delete show with confirmed bookings");
                }

                showRepository.delete(show);
                log.info("Show deleted: {}", id);
        }

        @Transactional
        @CacheEvict(value = { CacheConfig.SHOWS_CACHE, CacheConfig.SHOW_SEATS_CACHE }, allEntries = true)
        public ShowResponse updateShow(Long id, CreateShowRequest request) {
                log.info("Updating show with ID: {}", id);

                var show = showRepository.findById(id)
                                .orElseThrow(() -> new ShowNotFoundException(id));

                // 1. Check for existing bookings
                boolean hasBookings = bookingRepository.findAll().stream()
                                .anyMatch(b -> b.getShow().getId().equals(id)
                                                && b.getStatus() == Booking.BookingStatus.CONFIRMED);

                if (hasBookings) {
                        throw new ResourceConflictException("Cannot update show with existing confirmed bookings");
                }

                // 2. Update Basic Fields
                var movie = movieRepository.findById(request.movieId())
                                .orElseThrow(() -> new MovieNotFoundException(request.movieId()));
                show.setMovie(movie);

                // 3. Handle Hall Change (only if changed)
                if (!show.getHall().getId().equals(request.hallId())) {
                        var newHall = hallRepository.findById(request.hallId())
                                        .orElseThrow(() -> new HallNotFoundException(request.hallId()));
                        show.setHall(newHall);

                        // Delete old seats and create new ones
                        // Since we don't have cascade defined here, let's delete manually.
                        var oldSeats = showSeatRepository.findByShowId(id);
                        showSeatRepository.deleteAll(oldSeats);

                        // Create new seats
                        var hallSeats = seatRepository.findByHallId(newHall.getId());
                        var newShowSeats = new ArrayList<ShowSeat>();
                        for (var seat : hallSeats) {
                                if (seat.getStatus() == Seat.SeatStatus.MAINTENANCE)
                                        continue;

                                newShowSeats.add(ShowSeat.builder()
                                                .show(show)
                                                .seat(seat)
                                                .status(ShowSeat.SeatStatus.AVAILABLE)
                                                .price(new BigDecimal("15.00"))
                                                .version(0L)
                                                .build());
                        }
                        showSeatRepository.saveAll(newShowSeats);
                }

                // 4. Update Times
                if (request.endTime().isBefore(request.startTime())) {
                        throw new InvalidBookingException("End time must be after start time");
                }

                // Check for overlapping shows (excluding current show)
                List<Show> overlapping = showRepository.findOverlappingShows(request.hallId(), request.startTime(),
                                request.endTime());
                boolean hasOtherOverlaps = overlapping.stream()
                                .anyMatch(s -> !s.getId().equals(id));

                if (hasOtherOverlaps) {
                        throw new ResourceConflictException("Show time overlaps with existing shows in the same hall");
                }
                show.setStartTime(request.startTime());
                show.setEndTime(request.endTime());

                show = showRepository.save(show);
                log.info("Show updated successfully: {}", id);
                return mapToShowResponse(show);
        }

        // Analytics
        public Map<String, Object> getRevenueReport() {
                log.info("Generating revenue report");

                var allBookings = bookingRepository.findAll();
                var confirmedBookings = allBookings.stream()
                                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                                .toList();

                var totalRevenue = confirmedBookings.stream()
                                .map(Booking::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                var totalBookings = confirmedBookings.size();
                var totalSeatsBooked = confirmedBookings.stream()
                                .mapToLong(b -> b.getBookingSeats().size())
                                .sum();

                var cancelledBookings = allBookings.stream()
                                .filter(b -> b.getStatus() == Booking.BookingStatus.CANCELLED)
                                .count();

                return Map.of(
                                "totalRevenue", totalRevenue,
                                "totalBookings", totalBookings,
                                "totalSeatsBooked", totalSeatsBooked,
                                "cancelledBookings", cancelledBookings,
                                "averageBookingValue", totalBookings > 0
                                                ? totalRevenue.divide(BigDecimal.valueOf(totalBookings), 2,
                                                                RoundingMode.HALF_UP)
                                                : BigDecimal.ZERO);
        }

        public Map<String, Object> getPopularMovies() {
                log.info("Generating popular movies report");

                var allBookings = bookingRepository.findAll().stream()
                                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                                .toList();

                // Group bookings by movie
                var movieBookingCounts = allBookings.stream()
                                .collect(Collectors.groupingBy(
                                                b -> b.getShow().getMovie(),
                                                Collectors.counting()));

                // Sort by booking count
                var popularMovies = movieBookingCounts.entrySet().stream()
                                .sorted(Map.Entry.<Movie, Long>comparingByValue().reversed())
                                .limit(10)
                                .map(entry -> Map.of(
                                                "movieId", entry.getKey().getId(),
                                                "title", entry.getKey().getTitle(),
                                                "bookingCount", entry.getValue(),
                                                "genre", entry.getKey().getGenre()))
                                .toList();

                return Map.of(
                                "popularMovies", popularMovies,
                                "reportGeneratedAt", LocalDateTime.now());
        }

        public Map<String, Object> getShowOccupancy(Long showId) {
                log.info("Generating occupancy report for show ID: {}", showId);

                var show = showRepository.findById(showId)
                                .orElseThrow(() -> new ShowNotFoundException(showId));

                var showSeats = showSeatRepository.findByShowId(showId);
                var totalSeats = showSeats.size();
                var bookedSeats = showSeats.stream()
                                .filter(s -> s.getStatus() == ShowSeat.SeatStatus.BOOKED)
                                .count();
                var availableSeats = showSeats.stream()
                                .filter(s -> s.getStatus() == ShowSeat.SeatStatus.AVAILABLE)
                                .count();

                var occupancyRate = totalSeats > 0
                                ? (bookedSeats * 100.0) / totalSeats
                                : 0.0;

                return Map.of(
                                "showId", showId,
                                "movieTitle", show.getMovie().getTitle(),
                                "startTime", show.getStartTime(),
                                "totalSeats", totalSeats,
                                "bookedSeats", bookedSeats,
                                "availableSeats", availableSeats,
                                "occupancyRate", String.format("%.2f%%", occupancyRate));
        }

        // Helper methods
        private MovieResponse mapToMovieResponse(Movie movie) {
                return MovieResponse.builder()
                                .id(movie.getId())
                                .title(movie.getTitle())
                                .durationMinutes(movie.getDurationMinutes())
                                .genre(movie.getGenre())
                                .build();
        }

        private ShowResponse mapToShowResponse(Show show) {
                return ShowResponse.builder()
                                .id(show.getId())
                                .movieId(show.getMovie().getId())
                                .movieTitle(show.getMovie().getTitle())
                                .hallName(show.getHall().getName())
                                .startTime(show.getStartTime())
                                .endTime(show.getEndTime())
                                .build();
        }
}
