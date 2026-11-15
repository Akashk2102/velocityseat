package com.velocityseat.modules.booking.repository;

import com.velocityseat.modules.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.event e " +
           "JOIN FETCH e.venue v " +
           "WHERE b.user.id = :userId " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.event e " +
           "JOIN FETCH e.venue v " +
           "LEFT JOIN FETCH b.items items " +
           "LEFT JOIN FETCH items.eventSeat es " +
           "LEFT JOIN FETCH es.seat s " +
           "WHERE b.bookingReference = :bookingReference")
    Optional<Booking> findByBookingReferenceWithDetails(@Param("bookingReference") String bookingReference);
}
