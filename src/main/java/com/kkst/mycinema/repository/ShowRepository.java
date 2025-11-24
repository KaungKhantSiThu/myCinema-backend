package com.kkst.mycinema.repository;

import com.kkst.mycinema.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByMovieId(Long movieId);

    @Query("SELECT s FROM Show s WHERE s.movie.id = :movieId " +
           "AND DATE(s.startTime) = :date ORDER BY s.startTime")
    List<Show> findByMovieIdAndDate(@Param("movieId") Long movieId,
                                     @Param("date") LocalDate date);

    @Query("SELECT s FROM Show s WHERE s.startTime >= :now ORDER BY s.startTime")
    List<Show> findUpcomingShows(@Param("now") LocalDateTime now);
}

