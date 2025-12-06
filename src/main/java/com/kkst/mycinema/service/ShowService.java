package com.kkst.mycinema.service;

import com.kkst.mycinema.config.CacheConfig;
import com.kkst.mycinema.dto.SeatResponse;
import com.kkst.mycinema.dto.ShowResponse;
import com.kkst.mycinema.dto.ShowSeatsResponse;
import com.kkst.mycinema.entity.Show;
import com.kkst.mycinema.entity.ShowSeat;
import com.kkst.mycinema.exception.ShowNotFoundException;
import com.kkst.mycinema.repository.ShowRepository;
import com.kkst.mycinema.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;

    public List<ShowResponse> getShows(Long movieId, LocalDate date) {
        List<Show> shows;

        if (movieId != null && date != null) {
            shows = showRepository.findByMovieIdAndDate(movieId, date);
        } else if (movieId != null) {
            shows = showRepository.findByMovieId(movieId);
        } else {
            shows = showRepository.findAll();
        }

        return shows.stream()
                .map(this::mapToShowResponse)
                .toList();
    }

    @Cacheable(value = CacheConfig.SHOW_SEATS_CACHE, key = "#showId")
    public ShowSeatsResponse getShowSeats(Long showId) {
        var show = showRepository.findById(showId)
                .orElseThrow(() -> new ShowNotFoundException(showId));

        var showSeats = showSeatRepository.findByShowId(showId);

        // Group seats by row number for easy frontend rendering
        Map<Integer, List<SeatResponse>> seatsByRow = showSeats.stream()
                .map(this::mapToSeatResponse)
                .collect(Collectors.groupingBy(SeatResponse::rowNumber));

        return ShowSeatsResponse.builder()
                .showId(showId)
                .movieTitle(show.getMovie().getTitle())
                .seatsByRow(seatsByRow)
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

    private SeatResponse mapToSeatResponse(ShowSeat showSeat) {
        return SeatResponse.builder()
                .seatId(showSeat.getId())
                .rowNumber(showSeat.getSeat().getRowNumber())
                .seatNumber(showSeat.getSeat().getSeatNumber())
                .status(showSeat.getStatus().name())
                .price(showSeat.getPrice())
                .build();
    }
}

