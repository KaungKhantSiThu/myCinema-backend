package com.kkst.mycinema.repository;

import com.kkst.mycinema.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdOrderByBookingTimeDesc(Long userId);

    // Paginated version
    Page<Booking> findByUserIdOrderByBookingTimeDesc(Long userId, Pageable pageable);
}

