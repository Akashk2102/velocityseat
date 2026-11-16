package com.velocityseat.modules.admin.service;

import com.velocityseat.modules.admin.dto.AdminStatsResponse;
import com.velocityseat.modules.booking.model.Booking;
import com.velocityseat.modules.booking.model.BookingStatus;
import com.velocityseat.modules.booking.repository.BookingRepository;
import com.velocityseat.modules.event.model.EventSeat;
import com.velocityseat.modules.event.model.SeatStatus;
import com.velocityseat.modules.event.repository.EventRepository;
import com.velocityseat.modules.event.repository.EventSeatRepository;
import com.velocityseat.modules.reservation.model.ReservationStatus;
import com.velocityseat.modules.reservation.model.SeatReservation;
import com.velocityseat.modules.reservation.repository.SeatReservationRepository;
import com.velocityseat.modules.venue.repository.VenueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Mock
    private SeatReservationRepository seatReservationRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("Should accurately aggregate platform metrics")
    void getSystemStatistics_Success() {
        when(venueRepository.count()).thenReturn(5L);
        when(eventRepository.count()).thenReturn(12L);

        Booking b1 = Booking.builder().status(BookingStatus.CONFIRMED).totalAmount(new BigDecimal("150.00")).build();
        Booking b2 = Booking.builder().status(BookingStatus.CONFIRMED).totalAmount(new BigDecimal("300.00")).build();
        Booking b3 = Booking.builder().status(BookingStatus.CANCELLED).totalAmount(new BigDecimal("100.00")).build();
        when(bookingRepository.findAll()).thenReturn(List.of(b1, b2, b3));

        EventSeat s1 = EventSeat.builder().status(SeatStatus.BOOKED).build();
        EventSeat s2 = EventSeat.builder().status(SeatStatus.BOOKED).build();
        EventSeat s3 = EventSeat.builder().status(SeatStatus.AVAILABLE).build();
        when(eventSeatRepository.findAll()).thenReturn(List.of(s1, s2, s3));

        SeatReservation r1 = SeatReservation.builder()
                .status(ReservationStatus.ACTIVE)
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .build();
        when(seatReservationRepository.findAll()).thenReturn(List.of(r1));

        AdminStatsResponse stats = adminService.getSystemStatistics();

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalVenues()).isEqualTo(5L);
        assertThat(stats.getTotalEvents()).isEqualTo(12L);
        assertThat(stats.getTotalBookings()).isEqualTo(2L); // Only confirmed
        assertThat(stats.getTotalGrossRevenue()).isEqualByComparingTo("450.00");
        assertThat(stats.getTotalTicketsSold()).isEqualTo(2L);
        assertThat(stats.getActiveHoldsCount()).isEqualTo(1L);
    }
}
