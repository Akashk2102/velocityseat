package com.velocityseat.modules.reservation.service;

import com.velocityseat.common.exception.ConflictException;
import com.velocityseat.modules.event.model.*;
import com.velocityseat.modules.event.repository.EventRepository;
import com.velocityseat.modules.event.repository.EventSeatRepository;
import com.velocityseat.modules.reservation.dto.HoldSeatsRequest;
import com.velocityseat.modules.reservation.dto.ReservationResponse;
import com.velocityseat.modules.reservation.model.ReservationStatus;
import com.velocityseat.modules.reservation.model.SeatReservation;
import com.velocityseat.modules.reservation.repository.SeatReservationRepository;
import com.velocityseat.modules.user.model.Role;
import com.velocityseat.modules.user.model.User;
import com.velocityseat.modules.user.repository.UserRepository;
import com.velocityseat.modules.venue.model.Seat;
import com.velocityseat.modules.venue.model.SeatType;
import com.velocityseat.modules.venue.model.Venue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private RedisSeatLockService redisSeatLockService;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SeatReservationRepository seatReservationRepository;

    @InjectMocks
    private ReservationService reservationService;

    private User mockUser;
    private Event mockEvent;
    private EventSeat mockEventSeat;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("sarah@velocityseat.com")
                .role(Role.ROLE_CUSTOMER)
                .build();

        mockEvent = Event.builder()
                .id(10L)
                .title("Taylor Swift Eras Tour")
                .status(EventStatus.PUBLISHED)
                .build();

        Venue venue = Venue.builder().id(1L).name("Wembley").build();
        Seat seat = Seat.builder().id(101L).venue(venue).section("A").rowNumber("1").seatNumber("1").seatType(SeatType.VIP).build();

        mockEventSeat = EventSeat.builder()
                .id(501L)
                .event(mockEvent)
                .seat(seat)
                .price(new BigDecimal("150.00"))
                .status(SeatStatus.AVAILABLE)
                .version(0)
                .build();
    }

    @Test
    @DisplayName("Should successfully hold seats in Redis and update DB status to HELD")
    void holdSeats_Success() {
        HoldSeatsRequest request = HoldSeatsRequest.builder()
                .eventId(10L)
                .seatIds(List.of(501L))
                .build();

        when(userRepository.findByEmail("sarah@velocityseat.com")).thenReturn(Optional.of(mockUser));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(mockEvent));
        when(eventSeatRepository.findAllById(List.of(501L))).thenReturn(List.of(mockEventSeat));
        when(redisSeatLockService.acquireSeatHolds(eq(10L), eq(List.of(501L)), anyString(), eq(600L)))
                .thenReturn(true);

        ReservationResponse response = reservationService.holdSeats("sarah@velocityseat.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getReservationToken()).startsWith("res_");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("150.00");
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(mockEventSeat.getStatus()).isEqualTo(SeatStatus.HELD);

        verify(eventSeatRepository, times(1)).saveAll(anyList());
        verify(seatReservationRepository, times(1)).save(any(SeatReservation.class));
        verify(redisSeatLockService, times(1)).saveReservationPayload(anyString(), any(), eq(600L));
    }

    @Test
    @DisplayName("Should throw ConflictException if seat is already booked")
    void holdSeats_SeatAlreadyBooked_ThrowsConflict() {
        mockEventSeat.setStatus(SeatStatus.BOOKED);
        HoldSeatsRequest request = HoldSeatsRequest.builder()
                .eventId(10L)
                .seatIds(List.of(501L))
                .build();

        when(userRepository.findByEmail("sarah@velocityseat.com")).thenReturn(Optional.of(mockUser));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(mockEvent));
        when(eventSeatRepository.findAllById(List.of(501L))).thenReturn(List.of(mockEventSeat));

        assertThatThrownBy(() -> reservationService.holdSeats("sarah@velocityseat.com", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already booked");

        verify(redisSeatLockService, never()).acquireSeatHolds(any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("Should throw ConflictException if concurrent request wins Redis lock")
    void holdSeats_RedisLockFailed_ThrowsConflict() {
        HoldSeatsRequest request = HoldSeatsRequest.builder()
                .eventId(10L)
                .seatIds(List.of(501L))
                .build();

        when(userRepository.findByEmail("sarah@velocityseat.com")).thenReturn(Optional.of(mockUser));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(mockEvent));
        when(eventSeatRepository.findAllById(List.of(501L))).thenReturn(List.of(mockEventSeat));
        // Another thread locked the key in Redis first
        when(redisSeatLockService.acquireSeatHolds(eq(10L), eq(List.of(501L)), anyString(), eq(600L)))
                .thenReturn(false);

        assertThatThrownBy(() -> reservationService.holdSeats("sarah@velocityseat.com", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("just reserved by another customer");

        verify(seatReservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should release reservation and revert seats to AVAILABLE")
    void releaseReservation_Success() {
        mockEventSeat.setStatus(SeatStatus.HELD);
        SeatReservation reservation = SeatReservation.builder()
                .id(1L)
                .reservationToken("res_12345")
                .user(mockUser)
                .event(mockEvent)
                .status(ReservationStatus.ACTIVE)
                .build();

        when(seatReservationRepository.findByReservationToken("res_12345")).thenReturn(Optional.of(reservation));
        when(userRepository.findByEmail("sarah@velocityseat.com")).thenReturn(Optional.of(mockUser));
        when(redisSeatLockService.getReservationPayload("res_12345")).thenReturn(
                com.velocityseat.modules.reservation.dto.ReservationPayload.builder()
                        .seatIds(List.of(501L))
                        .build()
        );
        when(eventSeatRepository.findAllById(List.of(501L))).thenReturn(List.of(mockEventSeat));

        reservationService.releaseReservation("sarah@velocityseat.com", "res_12345");

        assertThat(mockEventSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(redisSeatLockService, times(1)).releaseSeatHolds(eq(10L), eq(List.of(501L)), eq("res_12345"));
        verify(eventSeatRepository, times(1)).saveAll(anyList());
    }
}
