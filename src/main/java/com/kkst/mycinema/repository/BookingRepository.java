package com.kkst.mycinema.repository;

import com.kkst.mycinema.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    // Optimized query with fetch joins to prevent N+1 problem
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.bookingSeats bs " +
           "LEFT JOIN FETCH bs.showSeat ss " +
           "LEFT JOIN FETCH ss.seat " +
           "LEFT JOIN FETCH b.show s " +
           "LEFT JOIN FETCH s.movie " +
           "WHERE b.user.id = :userId " +
           "ORDER BY b.bookingTime DESC")
    List<Booking> findByUserIdOrderByBookingTimeDesc(@Param("userId") Long userId);

    // Paginated version (Note: Fetch joins with pagination need special handling)
    @Query(value = "SELECT DISTINCT b FROM Booking b " +
                   "WHERE b.user.id = :userId " +
                   "ORDER BY b.bookingTime DESC",
           countQuery = "SELECT COUNT(DISTINCT b) FROM Booking b WHERE b.user.id = :userId")
    Page<Booking> findByUserIdOrderByBookingTimeDesc(@Param("userId") Long userId, Pageable pageable);

    // Fetch join for paginated results (called separately to avoid pagination issues)
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.bookingSeats bs " +
           "LEFT JOIN FETCH bs.showSeat ss " +
           "LEFT JOIN FETCH ss.seat " +
           "LEFT JOIN FETCH b.show s " +
           "LEFT JOIN FETCH s.movie " +
           "WHERE b.id IN :bookingIds")
    List<Booking> findByIdInWithDetails(@Param("bookingIds") List<Long> bookingIds);
}
