package com.kkst.mycinema.repository;

import com.kkst.mycinema.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ShowRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ShowRepository showRepository;

    private Movie testMovie;
    private Hall testHall;

    @BeforeEach
    void setUp() {
        testHall = Hall.builder()
                .name("Test Hall")
                .totalRows(10)
                .totalColumns(10)
                .build();
        testHall = entityManager.persist(testHall);

        testMovie = Movie.builder()
                .title("Test Movie")
                .durationMinutes(120)
                .genre("Action")
                .build();
        testMovie = entityManager.persist(testMovie);

        entityManager.flush();
    }

    @Test
    void findByMovieId_ReturnsShowsForMovie() {
        // Arrange
        Show show1 = Show.builder()
                .movie(testMovie)
                .hall(testHall)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .build();
        entityManager.persist(show1);

        Show show2 = Show.builder()
                .movie(testMovie)
                .hall(testHall)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(2))
                .build();
        entityManager.persist(show2);

        entityManager.flush();

        // Act
        List<Show> result = showRepository.findByMovieId(testMovie.getId());

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(s -> s.getMovie().getId().equals(testMovie.getId()));
    }

    @Test
    void findByMovieIdAndDate_ReturnsShowsForSpecificDate() {
        // Arrange
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        LocalDate tomorrowDate = tomorrow.toLocalDate();

        Show show1 = Show.builder()
                .movie(testMovie)
                .hall(testHall)
                .startTime(tomorrow.withHour(14).withMinute(0))
                .endTime(tomorrow.withHour(16).withMinute(0))
                .build();
        entityManager.persist(show1);

        Show show2 = Show.builder()
                .movie(testMovie)
                .hall(testHall)
                .startTime(tomorrow.withHour(18).withMinute(0))
                .endTime(tomorrow.withHour(20).withMinute(0))
                .build();
        entityManager.persist(show2);

        // Different date
        Show show3 = Show.builder()
                .movie(testMovie)
                .hall(testHall)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(2))
                .build();
        entityManager.persist(show3);

        entityManager.flush();

        // Act
        List<Show> result = showRepository.findByMovieIdAndDate(testMovie.getId(), tomorrowDate);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(s ->
            s.getStartTime().toLocalDate().equals(tomorrowDate)
        );
    }

    @Test
    void findUpcomingShows_ReturnsOnlyFutureShows() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        // Past show
        Show pastShow = Show.builder()
                .movie(testMovie)
                .hall(testHall)
                .startTime(now.minusDays(1))
                .endTime(now.minusDays(1).plusHours(2))
                .build();
        entityManager.persist(pastShow);

        // Future shows
        Show futureShow1 = Show.builder()
                .movie(testMovie)
                .hall(testHall)
                .startTime(now.plusHours(2))
                .endTime(now.plusHours(4))
                .build();
        entityManager.persist(futureShow1);

        Show futureShow2 = Show.builder()
                .movie(testMovie)
                .hall(testHall)
                .startTime(now.plusDays(1))
                .endTime(now.plusDays(1).plusHours(2))
                .build();
        entityManager.persist(futureShow2);

        entityManager.flush();

        // Act
        List<Show> result = showRepository.findUpcomingShows(now);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(s -> s.getStartTime().isAfter(now));
    }

    @Test
    void findByMovieId_EmptyResult_WhenNoShows() {
        // Act
        List<Show> result = showRepository.findByMovieId(999L);

        // Assert
        assertThat(result).isEmpty();
    }
}

