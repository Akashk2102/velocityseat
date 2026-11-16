package com.velocityseat.modules.admin.service;

import com.velocityseat.modules.admin.dto.AdminStatsResponse;
import com.velocityseat.modules.booking.model.Booking;
import com.velocityseat.modules.booking.model.BookingStatus;
import com.velocityseat.modules.booking.repository.BookingRepository;
import com.velocityseat.modules.event.model.SeatStatus;
import com.velocityseat.modules.event.repository.EventRepository;
import com.velocityseat.modules.event.repository.EventSeatRepository;
import com.velocityseat.modules.reservation.model.ReservationStatus;
import com.velocityseat.modules.reservation.repository.SeatReservationRepository;
import com.velocityseat.modules.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final EventSeatRepository eventSeatRepository;
    private final SeatReservationRepository seatReservationRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getSystemStatistics() {
        long totalVenues = venueRepository.count();
        long totalEvents = eventRepository.count();

        List<Booking> confirmedBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .toList();

        long totalBookings = confirmedBookings.size();
        BigDecimal totalGrossRevenue = confirmedBookings.stream()
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalTicketsSold = eventSeatRepository.findAll().stream()
                .filter(es -> es.getStatus() == SeatStatus.BOOKED)
                .count();

        long activeHoldsCount = seatReservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.ACTIVE && r.getExpiresAt().isAfter(Instant.now()))
                .count();

        return AdminStatsResponse.builder()
                .totalVenues(totalVenues)
                .totalEvents(totalEvents)
                .totalBookings(totalBookings)
                .totalTicketsSold(totalTicketsSold)
                .totalGrossRevenue(totalGrossRevenue)
                .activeHoldsCount(activeHoldsCount)
                .build();
    }
}
