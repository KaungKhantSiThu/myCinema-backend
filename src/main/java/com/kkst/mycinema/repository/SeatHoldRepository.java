package com.kkst.mycinema.repository;

import com.kkst.mycinema.entity.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    Optional<SeatHold> findByHoldToken(String holdToken);

    Optional<SeatHold> findByHoldTokenAndUserEmail(String holdToken, String userEmail);

    @Query("SELECT sh FROM SeatHold sh WHERE sh.holdToken = :token AND sh.user.email = :email")
    Optional<SeatHold> findByTokenAndUserEmail(@Param("token") String token, @Param("email") String email);

    /**
     * Find all expired holds that are still marked as ACTIVE
     */
    @Query("SELECT sh FROM SeatHold sh WHERE sh.status = 'ACTIVE' AND sh.expiresAt < :now")
    List<SeatHold> findExpiredHolds(@Param("now") LocalDateTime now);

    /**
     * Find active holds for a specific show
     */
    @Query("SELECT sh FROM SeatHold sh WHERE sh.show.id = :showId AND sh.status = 'ACTIVE' AND sh.expiresAt > :now")
    List<SeatHold> findActiveHoldsForShow(@Param("showId") Long showId, @Param("now") LocalDateTime now);

    /**
     * Find active holds by user
     */
    @Query("SELECT sh FROM SeatHold sh WHERE sh.user.id = :userId AND sh.status = 'ACTIVE' AND sh.expiresAt > :now")
    List<SeatHold> findActiveHoldsByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Bulk update expired holds to EXPIRED status
     */
    @Modifying
    @Query("UPDATE SeatHold sh SET sh.status = 'EXPIRED' WHERE sh.status = 'ACTIVE' AND sh.expiresAt < :now")
    int markExpiredHolds(@Param("now") LocalDateTime now);
}

