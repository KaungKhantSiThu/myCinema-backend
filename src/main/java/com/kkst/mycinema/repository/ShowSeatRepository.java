package com.kkst.mycinema.repository;

import com.kkst.mycinema.entity.ShowSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowId(Long showId);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT ss FROM ShowSeat ss WHERE ss.id IN :ids")
    List<ShowSeat> findByIdInWithLock(@Param("ids") List<Long> ids);

    @Query("SELECT ss FROM ShowSeat ss WHERE ss.show.id = :showId AND ss.id IN :seatIds")
    List<ShowSeat> findByShowIdAndIdIn(@Param("showId") Long showId,
                                        @Param("seatIds") List<Long> seatIds);
}

