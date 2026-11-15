package com.velocityseat.modules.venue.repository;

import com.velocityseat.modules.venue.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByVenueIdOrderBySectionAscRowNumberAscSeatNumberAsc(Long venueId);
    long countByVenueId(Long venueId);
    boolean existsByVenueIdAndSectionAndRowNumberAndSeatNumber(
            Long venueId, String section, String rowNumber, String seatNumber);
}
