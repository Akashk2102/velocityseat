package com.velocityseat.modules.reservation.repository;

import com.velocityseat.modules.reservation.model.ReservationStatus;
import com.velocityseat.modules.reservation.model.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

    Optional<SeatReservation> findByReservationToken(String reservationToken);

    @Query("SELECT r FROM SeatReservation r WHERE r.status = :status AND r.expiresAt < :now")
    List<SeatReservation> findExpiredReservations(
            @Param("status") ReservationStatus status,
            @Param("now") Instant now
    );
}
