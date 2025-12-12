package com.kkst.mycinema.config;

import com.kkst.mycinema.entity.*;
import com.kkst.mycinema.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final HallRepository hallRepository;
    private final SeatRepository seatRepository;
    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Only seed if database is empty
        if (hallRepository.count() > 0) {
            log.info("Database already seeded. Skipping data seeding.");
            return;
        }

        log.info("Starting data seeding...");

        // 0. Create default admin user for testing
        if (!userRepository.existsByEmail("admin@cinema.com")) {
            var admin = User.builder()
                    .name("Admin User")
                    .email("admin@cinema.com")
                    .password(passwordEncoder.encode("Admin123!"))
                    .roles("ROLE_ADMIN")
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(admin);
            log.info("Created default admin user: admin@cinema.com (password: Admin123!)");
        }

        // 1. Create Cinema Hall
        var hall = Hall.builder()
                .name("IMAX Hall 1")
                .totalRows(10)
                .totalColumns(10)
                .build();
        hall = hallRepository.save(hall);
        log.info("Created hall: {}", hall.getName());

        // 2. Create 100 Seats (10 rows x 10 columns)
        var seats = new ArrayList<Seat>();
        for (int row = 1; row <= 10; row++) {
            for (int col = 1; col <= 10; col++) {
                var seat = Seat.builder()
                        .hall(hall)
                        .rowNumber(row)
                        .seatNumber(col)
                        .build();
                seats.add(seat);
            }
        }
        seats = (ArrayList<Seat>) seatRepository.saveAll(seats);
        log.info("Created {} seats", seats.size());

        // 3. Create Movie
        var movie = Movie.builder()
                .title("Inception")
                .durationMinutes(148)
                .genre("Sci-Fi/Thriller")
                .build();
        movie = movieRepository.save(movie);
        log.info("Created movie: {}", movie.getTitle());

        // 4. Create Show (starting 2 hours from now)
        var now = LocalDateTime.now();
        var startTime = now.plusHours(2);
        var endTime = startTime.plusMinutes(movie.getDurationMinutes());

        var show = Show.builder()
                .movie(movie)
                .hall(hall)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        show = showRepository.save(show);
        log.info("Created show for {} at {}", movie.getTitle(), startTime);

        // 5. Create ShowSeats (100 seats for the show)
        var showSeats = new ArrayList<ShowSeat>();
        for (var seat : seats) {
            var showSeat = ShowSeat.builder()
                    .show(show)
                    .seat(seat)
                    .status(ShowSeat.SeatStatus.AVAILABLE)
                    .price(new BigDecimal("15.00"))
                    .version(0L)
                    .build();
            showSeats.add(showSeat);
        }
        showSeatRepository.saveAll(showSeats);
        log.info("Created {} show seats with AVAILABLE status", showSeats.size());

        log.info("Data seeding completed successfully!");
    }
}
